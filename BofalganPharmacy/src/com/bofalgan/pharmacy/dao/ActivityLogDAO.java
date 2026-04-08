package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.ActivityLog;
import com.bofalgan.pharmacy.model.Alert;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ActivityLogDAO {

    private final DatabaseManager db;

    public ActivityLogDAO(DatabaseManager db) { this.db = db; }

    public void insert(ActivityLog log) {
        String sql = """
            INSERT INTO activity_log
              (user_id, username, action, entity_type, entity_id, entity_name,
               old_value, new_value, changes_summary, ip_address)
            VALUES (?,?,?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, log.getUserId());
            ps.setString(2, log.getUsername());
            ps.setString(3, log.getAction());
            ps.setString(4, log.getEntityType());
            ps.setInt(5, log.getEntityId());
            ps.setString(6, log.getEntityName());
            ps.setString(7, log.getOldValue());
            ps.setString(8, log.getNewValue());
            ps.setString(9, log.getChangesSummary());
            ps.setString(10, log.getIpAddress());
            ps.executeUpdate();
        } catch (SQLException e) {
            // Log silently - don't crash app on audit failure
            System.err.println("[ActivityLog] Insert failed: " + e.getMessage());
        }
    }

    public List<ActivityLog> findAll(int limit) {
        String sql = "SELECT * FROM activity_log ORDER BY timestamp DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindAll activity_log failed: " + e.getMessage(), e);
        }
        return list;
    }

    public List<ActivityLog> findByUser(int userId, int limit) {
        String sql = "SELECT * FROM activity_log WHERE user_id=? ORDER BY timestamp DESC LIMIT ?";
        List<ActivityLog> list = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId); ps.setInt(2, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) list.add(mapRow(rs));
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindByUser activity_log failed: " + e.getMessage(), e);
        }
        return list;
    }

    private ActivityLog mapRow(ResultSet rs) throws SQLException {
        ActivityLog log = new ActivityLog();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setUsername(rs.getString("username"));
        log.setAction(rs.getString("action"));
        log.setEntityType(rs.getString("entity_type"));
        log.setEntityId(rs.getInt("entity_id"));
        log.setEntityName(rs.getString("entity_name"));
        log.setOldValue(rs.getString("old_value"));
        log.setNewValue(rs.getString("new_value"));
        log.setChangesSummary(rs.getString("changes_summary"));
        log.setIpAddress(rs.getString("ip_address"));
        Timestamp ts = rs.getTimestamp("timestamp");
        if (ts != null) log.setTimestamp(ts.toLocalDateTime());
        return log;
    }
}
