package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.Medicine;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MedicineDAO {

    private final DatabaseManager db;

    public MedicineDAO(DatabaseManager db) {
        this.db = db;
    }

    // ==================== INSERT ====================

    public int insert(Medicine m) {
        String sql = """
            INSERT INTO medicines
              (name, generic_name, category, strength, unit, batch_number,
               quantity, reorder_level, purchase_price, selling_price,
               supplier_id, expiry_date, storage_location,
               is_prescription_only, barcode)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getGenericName());
            ps.setString(3, m.getCategory());
            ps.setString(4, m.getStrength());
            ps.setString(5, m.getUnit());
            ps.setString(6, m.getBatchNumber());
            ps.setInt(7, m.getQuantity());
            ps.setInt(8, m.getReorderLevel());
            ps.setDouble(9, m.getPurchasePrice());
            ps.setDouble(10, m.getSellingPrice());
            if (m.getSupplierId() > 0)
                ps.setInt(11, m.getSupplierId());
            else
                ps.setNull(11, Types.INTEGER);
            ps.setDate(12, Date.valueOf(m.getExpiryDate()));
            ps.setString(13, m.getStorageLocation());
            ps.setBoolean(14, m.isPrescriptionOnly());
            ps.setString(15, m.getBarcode());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    m.setId(id);
                    return id;
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert medicine failed: " + e.getMessage(), e);
        }
        return -1;
    }

    // ==================== UPDATE ====================

    public void update(Medicine m) {
        String sql = """
            UPDATE medicines SET
              name=?, generic_name=?, category=?, strength=?, unit=?,
              quantity=?, reorder_level=?, purchase_price=?, selling_price=?,
              supplier_id=?, expiry_date=?, storage_location=?,
              is_prescription_only=?, barcode=?, updated_at=NOW()
            WHERE id=? AND is_deleted=0
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, m.getName());
            ps.setString(2, m.getGenericName());
            ps.setString(3, m.getCategory());
            ps.setString(4, m.getStrength());
            ps.setString(5, m.getUnit());
            ps.setInt(6, m.getQuantity());
            ps.setInt(7, m.getReorderLevel());
            ps.setDouble(8, m.getPurchasePrice());
            ps.setDouble(9, m.getSellingPrice());
            if (m.getSupplierId() > 0) ps.setInt(10, m.getSupplierId());
            else ps.setNull(10, Types.INTEGER);
            ps.setDate(11, Date.valueOf(m.getExpiryDate()));
            ps.setString(12, m.getStorageLocation());
            ps.setBoolean(13, m.isPrescriptionOnly());
            ps.setString(14, m.getBarcode());
            ps.setInt(15, m.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Update medicine failed: " + e.getMessage(), e);
        }
    }

    // ==================== SOFT DELETE ====================

    public void delete(int id) {
        String sql = "UPDATE medicines SET is_deleted=1, updated_at=NOW() WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Delete medicine failed: " + e.getMessage(), e);
        }
    }

    // ==================== QUERIES ====================

    public Medicine findById(int id) {
        String sql = """
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.id=? AND m.is_deleted=0
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindById medicine failed: " + e.getMessage(), e);
        }
        return null;
    }

    public Medicine findByBarcode(String barcode) {
        String sql = """
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.barcode=? AND m.is_deleted=0
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, barcode);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindByBarcode failed: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Medicine> findAll() {
        return queryList("""
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.is_deleted=0
            ORDER BY m.name ASC
        """);
    }

    public List<Medicine> search(String query) {
        String sql = """
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.is_deleted=0
              AND (m.name LIKE ? OR m.generic_name LIKE ? OR m.batch_number LIKE ? OR m.barcode LIKE ?)
            ORDER BY m.name ASC
        """;
        String q = "%" + query + "%";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q);
            ps.setString(3, q); ps.setString(4, q);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("Search medicine failed: " + e.getMessage(), e);
        }
    }

    public List<Medicine> findByCategory(String category) {
        String sql = """
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.is_deleted=0 AND m.category=?
            ORDER BY m.name ASC
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, category);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("FindByCategory failed: " + e.getMessage(), e);
        }
    }

    public List<Medicine> findExpiringSoon(int days) {
        String sql = """
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.is_deleted=0
              AND m.quantity > 0
              AND m.expiry_date <= DATE_ADD(CURDATE(), INTERVAL ? DAY)
            ORDER BY m.expiry_date ASC
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("FindExpiringSoon failed: " + e.getMessage(), e);
        }
    }

    public List<Medicine> findBelowReorderLevel() {
        String sql = """
            SELECT m.*, s.name AS supplier_name
            FROM medicines m
            LEFT JOIN suppliers s ON m.supplier_id = s.id
            WHERE m.is_deleted=0 AND m.quantity <= m.reorder_level
            ORDER BY m.quantity ASC
        """;
        return queryList(sql);
    }

    public void updateQuantity(int medicineId, int delta) {
        String sql = "UPDATE medicines SET quantity = quantity + ?, updated_at=NOW() WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, medicineId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("UpdateQuantity failed: " + e.getMessage(), e);
        }
    }

    public void updateQuantity(Connection conn, int medicineId, int delta) throws SQLException {
        String sql = "UPDATE medicines SET quantity = quantity + ?, updated_at=NOW() WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, delta);
            ps.setInt(2, medicineId);
            ps.executeUpdate();
        }
    }

    // ==================== COUNTS / STATS ====================

    public int countAll() {
        return countQuery("SELECT COUNT(*) FROM medicines WHERE is_deleted=0");
    }

    public int countLowStock() {
        return countQuery("SELECT COUNT(*) FROM medicines WHERE is_deleted=0 AND quantity <= reorder_level");
    }

    public int countExpiringSoon(int days) {
        String sql = "SELECT COUNT(*) FROM medicines WHERE is_deleted=0 AND quantity>0 "
                   + "AND expiry_date <= DATE_ADD(CURDATE(), INTERVAL " + days + " DAY)";
        return countQuery(sql);
    }

    public double getTotalInventoryValue() {
        String sql = "SELECT COALESCE(SUM(quantity * selling_price),0) FROM medicines WHERE is_deleted=0";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getDouble(1);
        } catch (SQLException e) {
            throw new DatabaseException("getTotalInventoryValue failed: " + e.getMessage(), e);
        }
        return 0;
    }

    public List<String> getAllCategories() {
        List<String> cats = new ArrayList<>();
        String sql = "SELECT name FROM medicine_categories WHERE is_active=1 ORDER BY name";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) cats.add(rs.getString(1));
        } catch (SQLException e) {
            throw new DatabaseException("getAllCategories failed: " + e.getMessage(), e);
        }
        return cats;
    }

    // ==================== PRIVATE HELPERS ====================

    private List<Medicine> queryList(String sql) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapResultSet(rs);
        } catch (SQLException e) {
            throw new DatabaseException("Query failed: " + e.getMessage(), e);
        }
    }

    private List<Medicine> mapResultSet(ResultSet rs) throws SQLException {
        List<Medicine> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        rs.close();
        return list;
    }

    private Medicine mapRow(ResultSet rs) throws SQLException {
        Medicine m = new Medicine();
        m.setId(rs.getInt("id"));
        m.setName(rs.getString("name"));
        m.setGenericName(rs.getString("generic_name"));
        m.setCategory(rs.getString("category"));
        m.setStrength(rs.getString("strength"));
        m.setUnit(rs.getString("unit"));
        m.setBatchNumber(rs.getString("batch_number"));
        m.setQuantity(rs.getInt("quantity"));
        m.setReorderLevel(rs.getInt("reorder_level"));
        m.setPurchasePrice(rs.getDouble("purchase_price"));
        m.setSellingPrice(rs.getDouble("selling_price"));
        m.setSupplierId(rs.getInt("supplier_id"));
        m.setSupplierName(rs.getString("supplier_name"));
        Date expiry = rs.getDate("expiry_date");
        if (expiry != null) m.setExpiryDate(expiry.toLocalDate());
        m.setStorageLocation(rs.getString("storage_location"));
        m.setPrescriptionOnly(rs.getBoolean("is_prescription_only"));
        m.setDeleted(rs.getBoolean("is_deleted"));
        m.setBarcode(rs.getString("barcode"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) m.setCreatedAt(ca.toLocalDateTime());
        Timestamp ua = rs.getTimestamp("updated_at");
        if (ua != null) m.setUpdatedAt(ua.toLocalDateTime());
        return m;
    }

    private int countQuery(String sql) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            throw new DatabaseException("Count query failed: " + e.getMessage(), e);
        }
        return 0;
    }
}
