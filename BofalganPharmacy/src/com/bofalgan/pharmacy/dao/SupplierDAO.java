package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.Supplier;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SupplierDAO {

    private final DatabaseManager db;

    public SupplierDAO(DatabaseManager db) { this.db = db; }

    public int insert(Supplier s) {
        String sql = """
            INSERT INTO suppliers (name, contact_person, phone, email, address, city, state, gstin, payment_terms, is_active)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, s.getName());
            ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getPhone());
            ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress());
            ps.setString(6, s.getCity());
            ps.setString(7, s.getState());
            ps.setString(8, s.getGstin());
            ps.setString(9, s.getPaymentTerms());
            ps.setBoolean(10, s.isActive());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { int id = keys.getInt(1); s.setId(id); return id; }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert supplier failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void update(Supplier s) {
        String sql = """
            UPDATE suppliers SET name=?, contact_person=?, phone=?, email=?,
            address=?, city=?, state=?, gstin=?, payment_terms=?, is_active=?
            WHERE id=?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.getName()); ps.setString(2, s.getContactPerson());
            ps.setString(3, s.getPhone()); ps.setString(4, s.getEmail());
            ps.setString(5, s.getAddress()); ps.setString(6, s.getCity());
            ps.setString(7, s.getState()); ps.setString(8, s.getGstin());
            ps.setString(9, s.getPaymentTerms()); ps.setBoolean(10, s.isActive());
            ps.setInt(11, s.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Update supplier failed: " + e.getMessage(), e);
        }
    }

    public void setActive(int id, boolean active) {
        String sql = "UPDATE suppliers SET is_active=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setBoolean(1, active); ps.setInt(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Supplier setActive failed: " + e.getMessage(), e);
        }
    }

    public Supplier findById(int id) {
        String sql = "SELECT * FROM suppliers WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindById supplier failed: " + e.getMessage(), e);
        }
        return null;
    }

    public List<Supplier> findAll() {
        return queryList("SELECT * FROM suppliers ORDER BY name");
    }

    public List<Supplier> findAllActive() {
        return queryList("SELECT * FROM suppliers WHERE is_active=1 ORDER BY name");
    }

    public List<Supplier> search(String query) {
        String sql = "SELECT * FROM suppliers WHERE name LIKE ? OR contact_person LIKE ? OR city LIKE ? ORDER BY name";
        String q = "%" + query + "%";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, q); ps.setString(2, q); ps.setString(3, q);
            return mapResultSet(ps.executeQuery());
        } catch (SQLException e) {
            throw new DatabaseException("Search supplier failed: " + e.getMessage(), e);
        }
    }

    private List<Supplier> queryList(String sql) {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            return mapResultSet(rs);
        } catch (SQLException e) {
            throw new DatabaseException("Query supplier failed: " + e.getMessage(), e);
        }
    }

    private List<Supplier> mapResultSet(ResultSet rs) throws SQLException {
        List<Supplier> list = new ArrayList<>();
        while (rs.next()) list.add(mapRow(rs));
        rs.close();
        return list;
    }

    private Supplier mapRow(ResultSet rs) throws SQLException {
        Supplier s = new Supplier();
        s.setId(rs.getInt("id")); s.setName(rs.getString("name"));
        s.setContactPerson(rs.getString("contact_person")); s.setPhone(rs.getString("phone"));
        s.setEmail(rs.getString("email")); s.setAddress(rs.getString("address"));
        s.setCity(rs.getString("city")); s.setState(rs.getString("state"));
        s.setGstin(rs.getString("gstin")); s.setPaymentTerms(rs.getString("payment_terms"));
        s.setActive(rs.getBoolean("is_active"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) s.setCreatedAt(ca.toLocalDateTime());
        return s;
    }
}
