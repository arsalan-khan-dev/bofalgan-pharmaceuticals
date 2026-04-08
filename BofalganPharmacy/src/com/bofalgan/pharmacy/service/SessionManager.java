package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.model.User;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Singleton that tracks the currently logged-in user and role.
 * All permission checks go through here.
 */
public class SessionManager {

    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime loginTime;
    private LocalDateTime lastActivityTime;

    // Permission map: role -> set of allowed actions
    private static final Map<String, String[]> ADMIN_PERMS = new HashMap<>();
    private static final Map<String, String[]> STAFF_PERMS = new HashMap<>();

    static {
        ADMIN_PERMS.put("ADMIN", new String[]{
            "VIEW_DASHBOARD", "ADD_MEDICINE", "EDIT_MEDICINE", "DELETE_MEDICINE",
            "VIEW_COST", "ADD_PURCHASE", "VIEW_PURCHASE", "CREATE_INVOICE",
            "VIEW_INVOICE", "VIEW_REPORTS", "VIEW_PROFIT", "MANAGE_USERS",
            "VIEW_SETTINGS", "MANAGE_SETTINGS", "BACKUP_RESTORE", "VIEW_AUDIT_LOG",
            "ADD_SUPPLIER", "EDIT_SUPPLIER", "DEACTIVATE_SUPPLIER",
            "MANAGE_CATEGORIES", "VIEW_ANALYTICS"
        });
        STAFF_PERMS.put("STAFF", new String[]{
            "VIEW_DASHBOARD", "ADD_MEDICINE", "EDIT_MEDICINE",
            "VIEW_INVOICE", "CREATE_INVOICE", "VIEW_ANALYTICS"
        });
    }

    private SessionManager() {}

    public static synchronized SessionManager getInstance() {
        if (instance == null) instance = new SessionManager();
        return instance;
    }

    public void startSession(User user) {
        this.currentUser      = user;
        this.loginTime        = LocalDateTime.now();
        this.lastActivityTime = LocalDateTime.now();
    }

    public void clearSession() {
        this.currentUser      = null;
        this.loginTime        = null;
        this.lastActivityTime = null;
    }

    public void updateActivity() {
        this.lastActivityTime = LocalDateTime.now();
    }

    public boolean isLoggedIn() {
        return currentUser != null;
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public String getCurrentRole() {
        return currentUser != null ? currentUser.getRole() : null;
    }

    public boolean isAdmin() {
        return currentUser != null && currentUser.isAdmin();
    }

    /**
     * Check if the current user has a specific permission.
     */
    public boolean hasPermission(String action) {
        if (currentUser == null) return false;
        String role = currentUser.getRole();
        if ("ADMIN".equals(role)) {
            for (String p : ADMIN_PERMS.get("ADMIN")) {
                if (p.equals(action)) return true;
            }
        } else if ("STAFF".equals(role)) {
            for (String p : STAFF_PERMS.get("STAFF")) {
                if (p.equals(action)) return true;
            }
        }
        return false;
    }

    /**
     * Returns true if session has timed out due to inactivity.
     */
    public boolean isSessionExpired() {
        int timeoutMins = AppConfig.SESSION_TIMEOUT_MINUTES;
        if (timeoutMins <= 0 || lastActivityTime == null) return false;
        return lastActivityTime.plusMinutes(timeoutMins).isBefore(LocalDateTime.now());
    }

    public LocalDateTime getLoginTime()        { return loginTime; }
    public LocalDateTime getLastActivityTime() { return lastActivityTime; }
}
