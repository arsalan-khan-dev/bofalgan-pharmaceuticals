package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.Invoice;
import com.bofalgan.pharmacy.model.InvoiceItem;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class InvoiceDAO {

    private final DatabaseManager db;

    public InvoiceDAO(DatabaseManager db) { this.db = db; }

    /**
     * Atomically creates an invoice with all its items.
     * Also decrements medicine quantities.
     * Returns generated invoice id.
     */
    public int create(Invoice inv, MedicineDAO medicineDAO) {
        int[] genId = {-1};
        db.withTransaction(conn -> {
            // 1. Generate invoice number
            String invNum = generateInvoiceNumber(conn);
            inv.setInvoiceNumber(invNum);

            // 2. Insert invoice header
            String sql = """
                INSERT INTO invoices
                  (invoice_number, customer_name, customer_phone, customer_email,
                   subtotal, discount_type, discount_value, tax_amount,
                   total_amount, paid_amount, payment_method, payment_status,
                   notes, created_by_user_id)
                VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setString(1, invNum);
                ps.setString(2, inv.getCustomerName());
                ps.setString(3, inv.getCustomerPhone());
                ps.setString(4, inv.getCustomerEmail());
                ps.setDouble(5, inv.getSubtotal());
                ps.setString(6, inv.getDiscountType());
                ps.setDouble(7, inv.getDiscountValue());
                ps.setDouble(8, inv.getTaxAmount());
                ps.setDouble(9, inv.getTotalAmount());
                ps.setDouble(10, inv.getPaidAmount());
                ps.setString(11, inv.getPaymentMethod());
                ps.setString(12, inv.getPaymentStatus());
                ps.setString(13, inv.getNotes());
                ps.setInt(14, inv.getCreatedByUserId());
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) {
                        genId[0] = keys.getInt(1);
                        inv.setId(genId[0]);
                    }
                }
            }

            // 3. Insert items and deduct stock
            String itemSql = """
                INSERT INTO invoice_items (invoice_id, medicine_id, batch_number, quantity, unit_price, line_total)
                VALUES (?,?,?,?,?,?)
            """;
            try (PreparedStatement ps = conn.prepareStatement(itemSql)) {
                for (InvoiceItem item : inv.getItems()) {
                    item.setInvoiceId(genId[0]);
                    ps.setInt(1, genId[0]);
                    ps.setInt(2, item.getMedicineId());
                    ps.setString(3, item.getBatchNumber());
                    ps.setInt(4, item.getQuantity());
                    ps.setDouble(5, item.getUnitPrice());
                    ps.setDouble(6, item.getLineTotal());
                    ps.addBatch();

                    // Deduct stock
                    medicineDAO.updateQuantity(conn, item.getMedicineId(), -item.getQuantity());
                }
                ps.executeBatch();
            }
        });
        return genId[0];
    }

    public Invoice findById(int id) {
        String sql = """
            SELECT i.*, u.full_name AS created_by_name
            FROM invoices i
            LEFT JOIN users u ON i.created_by_user_id = u.id
            WHERE i.id=?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Invoice inv = mapRow(rs);
                    inv.setItems(findItemsByInvoiceId(id));
                    return inv;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindById invoice failed: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Invoice> findAll(int limit, int offset) {
        String sql = """
            SELECT i.*, u.full_name AS created_by_name
            FROM invoices i
            LEFT JOIN users u ON i.created_by_user_id = u.id
            ORDER BY i.created_at DESC
            LIMIT ? OFFSET ?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit); ps.setInt(2, offset);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("FindAll invoices failed: " + e.getMessage(), e);
        }
    }

    public List<Invoice> findByDateRange(LocalDate start, LocalDate end) {
        String sql = """
            SELECT i.*, u.full_name AS created_by_name
            FROM invoices i
            LEFT JOIN users u ON i.created_by_user_id = u.id
            WHERE DATE(i.created_at) BETWEEN ? AND ?
            ORDER BY i.created_at DESC
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("FindByDateRange invoices failed: " + e.getMessage(), e);
        }
    }

    public List<InvoiceItem> findItemsByInvoiceId(int invoiceId) {
        String sql = """
            SELECT ii.*, m.name AS medicine_name
            FROM invoice_items ii
            LEFT JOIN medicines m ON ii.medicine_id = m.id
            WHERE ii.invoice_id=?
        """;
        List<InvoiceItem> items = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, invoiceId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvoiceItem item = new InvoiceItem();
                    item.setId(rs.getInt("id"));
                    item.setInvoiceId(invoiceId);
                    item.setMedicineId(rs.getInt("medicine_id"));
                    item.setMedicineName(rs.getString("medicine_name"));
                    item.setBatchNumber(rs.getString("batch_number"));
                    item.setQuantity(rs.getInt("quantity"));
                    item.setUnitPrice(rs.getDouble("unit_price"));
                    item.setLineTotal(rs.getDouble("line_total"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindItemsByInvoiceId failed: " + e.getMessage(), e);
        }
        return items;
    }

    public int countTotal() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM invoices")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Count invoices failed: " + e.getMessage(), e);
        }
        return 0;
    }

    public double getTodayRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount),0) FROM invoices WHERE DATE(created_at)=CURDATE()";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            throw new DatabaseException("getTodayRevenue failed: " + e.getMessage(), e);
        }
        return 0;
    }

    public double getMonthRevenue() {
        String sql = "SELECT COALESCE(SUM(total_amount),0) FROM invoices "
                   + "WHERE MONTH(created_at)=MONTH(NOW()) AND YEAR(created_at)=YEAR(NOW())";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            throw new DatabaseException("getMonthRevenue failed: " + e.getMessage(), e);
        }
        return 0;
    }

    // ==================== PRIVATE ====================

    private String generateInvoiceNumber(Connection conn) throws SQLException {
        String sql = "SELECT COUNT(*) FROM invoices WHERE YEAR(created_at)=YEAR(NOW())";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
            int count = rs.next() ? rs.getInt(1) + 1 : 1;
            int year = java.time.Year.now().getValue();
            return String.format("INV-%d-%05d", year, count);
        }
    }

    private List<Invoice> mapResultSet(ResultSet rs) throws SQLException {
        List<Invoice> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        rs.close();
        return list;
    }

    private Invoice mapRow(ResultSet rs) throws SQLException {
        Invoice inv = new Invoice();
        inv.setId(rs.getInt("id"));
        inv.setInvoiceNumber(rs.getString("invoice_number"));
        inv.setCustomerName(rs.getString("customer_name"));
        inv.setCustomerPhone(rs.getString("customer_phone"));
        inv.setCustomerEmail(rs.getString("customer_email"));
        inv.setSubtotal(rs.getDouble("subtotal"));
        inv.setDiscountType(rs.getString("discount_type"));
        inv.setDiscountValue(rs.getDouble("discount_value"));
        inv.setTaxAmount(rs.getDouble("tax_amount"));
        inv.setTotalAmount(rs.getDouble("total_amount"));
        inv.setPaidAmount(rs.getDouble("paid_amount"));
        inv.setPaymentMethod(rs.getString("payment_method"));
        inv.setPaymentStatus(rs.getString("payment_status"));
        inv.setNotes(rs.getString("notes"));
        inv.setCreatedByUserId(rs.getInt("created_by_user_id"));
        try { inv.setCreatedByName(rs.getString("created_by_name")); } catch (SQLException ignored) {}
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) inv.setCreatedAt(ca.toLocalDateTime());
        return inv;
    }
}
