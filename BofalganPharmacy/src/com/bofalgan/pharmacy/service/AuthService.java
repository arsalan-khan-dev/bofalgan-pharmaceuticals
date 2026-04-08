package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.dao.ActivityLogDAO;
import com.bofalgan.pharmacy.dao.UserDAO;
import com.bofalgan.pharmacy.model.ActivityLog;
import com.bofalgan.pharmacy.model.User;
import com.bofalgan.pharmacy.util.ValidationException;
import org.mindrot.jbcrypt.BCrypt;

import java.time.LocalDateTime;

public class AuthService {

    private final UserDAO userDAO;
    private final ActivityLogDAO logDAO;
    private final SessionManager session;

    public AuthService(UserDAO userDAO, ActivityLogDAO logDAO) {
        this.userDAO = userDAO;
        this.logDAO  = logDAO;
        this.session = SessionManager.getInstance();
    }

    /**
     * Authenticates user. Returns true on success.
     * Throws ValidationException with descriptive message on failure.
     */
    public boolean login(String username, String password) {
        if (username == null || username.isBlank())
            throw new ValidationException("username", "Username is required.");
        if (password == null || password.isBlank())
            throw new ValidationException("password", "Password is required.");

        User user = userDAO.findByUsername(username.trim());
        if (user == null) {
            throw new ValidationException("Invalid username or password.");
        }

        if (!user.isActive()) {
            throw new ValidationException("Your account has been deactivated. Contact administrator.");
        }

        if (user.isLocked()) {
            throw new ValidationException("Account locked until " + user.getLockedUntil() +
                ". Too many failed login attempts.");
        }

        boolean passwordMatch = BCrypt.checkpw(password, user.getPasswordHash());
        if (!passwordMatch) {
            userDAO.incrementFailedLogins(user.getId());
            int remaining = AppConfig.MAX_FAILED_LOGINS - (user.getFailedLoginAttempts() + 1);
            if (remaining <= 0) {
                LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(AppConfig.LOCK_DURATION_MINUTES);
                userDAO.lockUser(user.getId(), lockUntil);
                throw new ValidationException("Account locked for " + AppConfig.LOCK_DURATION_MINUTES +
                    " minutes after too many failed attempts.");
            }
            throw new ValidationException("Invalid username or password. " + remaining + " attempt(s) remaining.");
        }

        // Successful login
        userDAO.updateLastLogin(user.getId());
        session.startSession(user);

        ActivityLog log = new ActivityLog(user.getId(), user.getUsername(),
            "LOGIN", "USER", user.getId(), user.getUsername(), "User logged in.");
        logDAO.insert(log);

        return true;
    }

    public void logout() {
        if (session.isLoggedIn()) {
            User user = session.getCurrentUser();
            ActivityLog log = new ActivityLog(user.getId(), user.getUsername(),
                "LOGOUT", "USER", user.getId(), user.getUsername(), "User logged out.");
            logDAO.insert(log);
        }
        session.clearSession();
    }

    /**
     * Changes the current user's password after verifying old password.
     */
    public void changePassword(String oldPassword, String newPassword, String confirmPassword) {
        User user = session.getCurrentUser();
        if (user == null) throw new ValidationException("Not logged in.");

        if (!BCrypt.checkpw(oldPassword, user.getPasswordHash()))
            throw new ValidationException("oldPassword", "Current password is incorrect.");

        validatePasswordStrength(newPassword);

        if (!newPassword.equals(confirmPassword))
            throw new ValidationException("confirmPassword", "Passwords do not match.");

        String newHash = BCrypt.hashpw(newPassword, BCrypt.gensalt(AppConfig.BCRYPT_ROUNDS));
        userDAO.updatePassword(user.getId(), newHash);

        ActivityLog log = new ActivityLog(user.getId(), user.getUsername(),
            "UPDATE", "USER", user.getId(), user.getUsername(), "Password changed.");
        logDAO.insert(log);
    }

    /**
     * Admin resets another user's password.
     */
    public String adminResetPassword(int userId) {
        if (!session.isAdmin()) throw new ValidationException("Admin access required.");
        String tempPass = "Temp@" + (int)(Math.random() * 90000 + 10000);
        String hash = BCrypt.hashpw(tempPass, BCrypt.gensalt(AppConfig.BCRYPT_ROUNDS));
        userDAO.updatePassword(userId, hash);
        return tempPass;
    }

    public static String hashPassword(String plaintext) {
        return BCrypt.hashpw(plaintext, BCrypt.gensalt(AppConfig.BCRYPT_ROUNDS));
    }

    public static void validatePasswordStrength(String password) {
        if (password == null || password.length() < 8)
            throw new ValidationException("password", "Password must be at least 8 characters.");
        if (!password.matches(".*[A-Z].*"))
            throw new ValidationException("password", "Password must contain at least one uppercase letter.");
        if (!password.matches(".*[0-9].*"))
            throw new ValidationException("password", "Password must contain at least one digit.");
        if (!password.matches(".*[!@#$%^&*()_+\\-=\\[\\]{}].*"))
            throw new ValidationException("password", "Password must contain at least one special character.");
    }
}
