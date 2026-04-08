package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.Purchase;
import com.bofalgan.pharmacy.model.PurchaseItem;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class PurchaseDAO {

    private final DatabaseManager db;

    public PurchaseDAO(DatabaseManager db) { this.db = db; }

    public int create(Purchase p, MedicineDAO medicineDAO) {
        int[] genId = {-1};
        db.withTransaction(conn -> {
            String sql = """
                INSERT INTO purchases
                  (supplier_id, purchase_date, delivery_date, total_amount,
                   tax_amount, discount_amount, notes, payment_status,
                   paid_amount, created_by_user_id)
                VALUES (?,?,?,?,?,?,?,?,?,?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, p.getSupplierId());
                ps.setDate(2, Date.valueOf(p.getPurchaseDate()));
                if (p.getDeliveryDate() != null)
                    ps.setDate(3, Date.valueOf(p.getDeliveryDate()));
                else ps.setNull(3, Types.DATE);
                ps.setDouble(4, p.getTotalAmount());
                ps.setDouble(5, p.getTaxAmount());
                ps.setDouble(6, p.getDiscountAmount());
                ps.setString(7, p.getNotes());
                ps.setString(8, p.getPaymentStatus());
                ps.setDouble(9, p.getPaidAmount());
                ps.setInt(10, p.getCreatedByUserId());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) { genId[0] = keys.getInt(1); p.setId(genId[0]); }
                }
            }

            String itemSql = """
                INSERT INTO purchase_items
                  (purchase_id, medicine_id, batch_number, quantity_received,
                   quantity_accepted, unit_price, expiry_date, notes)
                VALUES (?,?,?,?,?,?,?,?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (PurchaseItem item : p.getItems()) {
                    item.setPurchaseId(genId[0]);
                    ps.setInt(1, genId[0]);
                    ps.setInt(2, item.getMedicineId());
                    ps.setString(3, item.getBatchNumber());
                    ps.setInt(4, item.getQuantityReceived());
                    ps.setInt(5, item.getQuantityAccepted());
                    ps.setDouble(6, item.getUnitPrice());
                    ps.setDate(7, Date.valueOf(item.getExpiryDate()));
                    ps.setString(8, item.getNotes());
                    ps.addBatch();

                    // Update inventory
                    medicineDAO.updateQuantity(conn, item.getMedicineId(), item.getQuantityAccepted());
                }
                ps.executeBatch();
            }
        });
        return genId[0];
    }

    public void updatePaymentStatus(int purchaseId, String status, double paidAmount) {
        String sql = "UPDATE purchases SET payment_status=?, paid_amount=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status); ps.setDouble(2, paidAmount); ps.setInt(3, purchaseId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("UpdatePaymentStatus failed: " + e.getMessage(), e);
        }
    }

    public Purchase findById(int id) {
        String sql = """
            SELECT p.*, s.name AS supplier_name, u.full_name AS created_by_name
            FROM purchases p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN users u ON p.created_by_user_id = u.id
            WHERE p.id=?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Purchase p = mapRow(rs);
                    p.setItems(findItemsByPurchaseId(id));
                    return p;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindById purchase failed: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Purchase> findAll() {
        String sql = """
            SELECT p.*, s.name AS supplier_name, u.full_name AS created_by_name
            FROM purchases p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN users u ON p.created_by_user_id = u.id
            ORDER BY p.created_at DESC
        """;
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapResultSet(rs);
        } catch (SQLException e) {
            throw new DatabaseException("FindAll purchases failed: " + e.getMessage(), e);
        }
    }

    public List<Purchase> findByDateRange(LocalDate start, LocalDate end) {
        String sql = """
            SELECT p.*, s.name AS supplier_name, u.full_name AS created_by_name
            FROM purchases p
            LEFT JOIN suppliers s ON p.supplier_id = s.id
            LEFT JOIN users u ON p.created_by_user_id = u.id
            WHERE p.purchase_date BETWEEN ? AND ?
            ORDER BY p.purchase_date DESC
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start)); ps.setDate(2, Date.valueOf(end));
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("FindByDateRange purchases failed: " + e.getMessage(), e);
        }
    }

    public List<PurchaseItem> findItemsByPurchaseId(int purchaseId) {
        String sql = """
            SELECT pi.*, m.name AS medicine_name
            FROM purchase_items pi
            LEFT JOIN medicines m ON pi.medicine_id = m.id
            WHERE pi.purchase_id=?
        """;
        List<PurchaseItem> items = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, purchaseId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    PurchaseItem item = new PurchaseItem();
                    item.setId(rs.getInt("id"));
                    item.setPurchaseId(purchaseId);
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setBatchNumber(rs.getString("batch_number"));
                    item.setQuantityReceived(rs.getInt("quantity_received"));
                    item.setQuantityAccepted(rs.getInt("quantity_accepted"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    Date exp = rs.getDate("expiry_date");
                    if (exp != null) item.setExpiryDate(exp.toLocalDate());
                    item.setNotes(rs.getString("notes"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindItemsByPurchaseId failed: " + e.getMessage(), e);
        }
        return items;
    }

    private List<Purchase> mapResultSet(ResultSet rs) throws SQLException {
        List<Purchase> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        rs.close();
        return list;
    }

    private Purchase mapRow(ResultSet rs) throws SQLException {
        Purchase p = new Purchase();
        p.setId(rs.getInt("id"));
        p.setSupplierId(rs.getInt("supplier_id"));
        p.setSupplierName(rs.getString("supplier_name"));
        Date pd = rs.getDate("purchase_date");
        if (pd != null) p.setPurchaseDate(pd.toLocalDate());
        Date dd = rs.getDate("delivery_date");
        if (dd != null) p.setDeliveryDate(dd.toLocalDate());
        p.setTotalAmount(rs.getDouble("total_amount"));
        p.setTaxAmount(rs.getDouble("tax_amount"));
        p.setDiscountAmount(rs.getDouble("discount_amount"));
        p.setNotes(rs.getString("notes"));
        p.setPaymentStatus(rs.getString("payment_status"));
        p.setPaidAmount(rs.getDouble("paid_amount"));
        p.setCreatedByUserId(rs.getInt("created_by_user_id"));
        try { p.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) p.setCreatedAt(ca.toLocalDateTime());
        return p;
    }
}
