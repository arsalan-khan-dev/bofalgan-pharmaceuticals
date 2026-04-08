package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.model.User;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private final DatabaseManager db;

    public UserDAO(DatabaseManager db) { this.db = db; }

    public int insert(User u) {
        String sql = """
            INSERT INTO users (username, password_hash, full_name, role, email, phone, is_active, created_by_user_id)
            VALUES (?,?,?,?,?,?,?,?)
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, u.getUsername());
            ps.setString(2, u.getPasswordHash());
            ps.setString(3, u.getFullName());
            ps.setString(4, u.getRole());
            ps.setString(5, u.getEmail());
            ps.setString(6, u.getPhone());
            ps.setBoolean(7, u.isActive());
            ps.setInt(8, u.getCreatedByUserId());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) { int id = keys.getInt(1); u.setId(id); return id; }
            }
        } catch (SQLException e) {
            throw new DatabaseException("Insert user failed: " + e.getMessage(), e);
        }
        return -1;
    }

    public void update(User u) {
        String sql = """
            UPDATE users SET full_name=?, email=?, phone=?, role=?, is_active=?
            WHERE id=?
        """;
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, u.getFullName());
            ps.setString(2, u.getEmail());
            ps.setString(3, u.getPhone());
            ps.setString(4, u.getRole());
            ps.setBoolean(5, u.isActive());
            ps.setInt(6, u.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("Update user failed: " + e.getMessage(), e);
        }
    }

    public void updatePassword(int userId, String newHash) {
        String sql = "UPDATE users SET password_hash=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newHash);
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("UpdatePassword failed: " + e.getMessage(), e);
        }
    }

    public void updateLastLogin(int userId) {
        String sql = "UPDATE users SET last_login=NOW(), failed_login_attempts=0, locked_until=NULL WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("UpdateLastLogin failed: " + e.getMessage(), e);
        }
    }

    public void incrementFailedLogins(int userId) {
        String sql = "UPDATE users SET failed_login_attempts = failed_login_attempts + 1 WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("IncrementFailedLogins failed: " + e.getMessage(), e);
        }
    }

    public void lockUser(int userId, LocalDateTime lockedUntil) {
        String sql = "UPDATE users SET locked_until=? WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.valueOf(lockedUntil));
            ps.setInt(2, userId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new DatabaseException("LockUser failed: " + e.getMessage(), e);
        }
    }

    public User findByUsername(String username) {
        String sql = "SELECT * FROM users WHERE username=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindByUsername failed: " + e.getMessage(), e);
        }
        return null;
    }

    public User findById(int id) {
        String sql = "SELECT * FROM users WHERE id=?";
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        } catch (SQLException e) {
            throw new DatabaseException("FindById user failed: " + e.getMessage(), e);
        }
        return null;
    }

    public List<User> findAll() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM users ORDER BY username";
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        } catch (SQLException e) {
            throw new DatabaseException("FindAll users failed: " + e.getMessage(), e);
        }
        return list;
    }

    private User mapRow(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setUsername(rs.getString("username"));
        u.setPasswordHash(rs.getString("password_hash"));
        u.setFullName(rs.getString("full_name"));
        u.setRole(rs.getString("role"));
        u.setEmail(rs.getString("email"));
        u.setPhone(rs.getString("phone"));
        u.setActive(rs.getBoolean("is_active"));
        u.setFailedLoginAttempts(rs.getInt("failed_login_attempts"));
        Timestamp ll = rs.getTimestamp("last_login");
        if (ll != null) u.setLastLogin(ll.toLocalDateTime());
        Timestamp lu = rs.getTimestamp("locked_until");
        if (lu != null) u.setLockedUntil(lu.toLocalDateTime());
        Timestamp ca = rs.getTimestamp("created_at");
        if (ca != null) u.setCreatedAt(ca.toLocalDateTime());
        u.setCreatedByUserId(rs.getInt("created_by_user_id"));
        return u;
    }
}
