package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.Alert;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AlertDAO {

    private final DatabaseManager db;

    public AlertDAO(DatabaseManager db) { this.db = db; }

    public int insert(Alert a) {
        String sql = """
            INSERT INTO alerts (alert_type, severity, entity_type, entity_id, entity_name, message)
            VALUES (?,?,?,?,?,?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, a.getAlertType());
            ps.setString(2, a.getSeverity());
            ps.setString(3, a.getEntityType());
            ps.setInt(4, a.getEntityId());
            ps.setString(5, a.getEntityName());
            ps.setString(6, a.getMessage());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { int id = keys.getInt(1); a.setId(id); return id; }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert alert failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void dismiss(int alertId, int userId) {
        String sql = "UPDATE alerts SET is_dismissed=1, dismissed_at=NOW(), dismissed_by_user_id=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, alertId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Dismiss alert failed: " + e.getMessage(), e);
        }
    }

    public void clearEntityAlerts(String entityType, int entityId) {
        String sql = "UPDATE alerts SET is_dismissed=1 WHERE entity_type=? AND entity_id=? AND is_dismissed=0";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, entityType); ps.setInt(2, entityId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[AlertDAO] clearEntityAlerts: " + e.getMessage());
        }
    }

    public List<Alert> findActive() {
        String sql = "SELECT * FROM alerts WHERE is_dismissed=0 ORDER BY created_at DESC";
        return queryList(sql);
    }

    public int countActive() {
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM alerts WHERE is_dismissed=0")) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[AlertDAO] countActive: " + e.getMessage());
        }
        return 0;
    }

    public int countActiveCritical() {
        String sql = "SELECT COUNT(*) FROM alerts WHERE is_dismissed=0 AND severity='CRITICAL'";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            if (rs.next()) return rs.getInt(1);
        } catch (SQLException e) {
            System.err.println("[AlertDAO] countActiveCritical: " + e.getMessage());
        }
        return 0;
    }

    private List<Alert> queryList(String sql) {
        List<Alert> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("Query alerts failed: " + e.getMessage(), e);
        }
        return list;
    }

    private Alert mapRow(ResultSet rs) throws SQLException {
        Alert a = new Alert();
        a.setId(rs.getInt("id"));
        a.setAlertType(rs.getString("alert_type"));
        a.setSeverity(rs.getString("severity"));
        a.setEntityType(rs.getString("entity_type"));
        a.setEntityId(rs.getInt("entity_id"));
        a.setEntityName(rs.getString("entity_name"));
        a.setMessage(rs.getString("message"));
        a.setDismissed(rs.getBoolean("is_dismissed"));
        Timestamp da = rs.getTimestamp("dismissed_at");
        if (da != null) a.setDismissedAt(da.toLocalDateTime());
        a.setDismissedByUserId(rs.getInt("dismissed_by_user_id"));
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) a.setCreatedAt(ca.toLocalDateTime());
        return a;
    }
}
