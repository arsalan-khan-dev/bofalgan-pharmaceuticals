package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.dao.ActivityLogDAO;
import com.bofalgan.pharmacy.dao.UserDAO;
import com.bofalgan.pharmacy.model.ActivityLog;
import com.bofalgan.pharmacy.model.User;
import com.bofalgan.pharmacy.storage.FileStorageManager;
import com.bofalgan.pharmacy.util.ValidationException;

import java.util.List;

public class UserService {

    private final UserDAO        userDAO;
    private final ActivityLogDAO logDAO;
    private final FileStorageManager fileStorage;
    private final SessionManager session;

    public UserService(UserDAO userDAO, ActivityLogDAO logDAO) {
        this.userDAO     = userDAO;
        this.logDAO      = logDAO;
        this.fileStorage = FileStorageManager.getInstance();
        this.session     = SessionManager.getInstance();
    }

    public User addUser(User user) {
        if (!session.isAdmin()) throw new ValidationException("Only admins can add users.");
        validateUser(user);

        if (userDAO.findByUsername(user.getUsername()) != null)
            throw new ValidationException("username", "Username '" + user.getUsername() + "' already exists.");

        String hash = AuthService.hashPassword(user.getPasswordHash());
        user.setPasswordHash(hash);
        user.setCreatedByUserId(session.getCurrentUser().getId());

        int id = userDAO.insert(user);
        user.setId(id);

        fileStorage.upsertRecord(AppConfig.USERS_FILE, user, id);

        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "CREATE", "USER", id, user.getUsername(),
            "Created user: " + user.getUsername() + " (" + user.getRole() + ")"
        ));
        return user;
    }

    public void updateUser(User user) {
        if (!session.isAdmin()) throw new ValidationException("Only admins can edit users.");
        validateUser(user);
        userDAO.update(user);
        fileStorage.upsertRecord(AppConfig.USERS_FILE, user, user.getId());

        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "UPDATE", "USER", user.getId(), user.getUsername(),
            "Updated user: " + user.getUsername()
        ));
    }

    public void deactivateUser(int userId) {
        if (!session.isAdmin()) throw new ValidationException("Only admins can deactivate users.");
        User user = userDAO.findById(userId);
        if (user == null) throw new ValidationException("User not found.");
        if (userId == session.getCurrentUser().getId())
            throw new ValidationException("Cannot deactivate your own account.");
        user.setActive(false);
        userDAO.update(user);
        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "UPDATE", "USER", userId, user.getUsername(),
            "Deactivated user: " + user.getUsername()
        ));
    }

    public List<User> getAllUsers() { return userDAO.findAll(); }
    public User getUserById(int id) { return userDAO.findById(id); }

    private void validateUser(User u) {
        if (u.getUsername() == null || u.getUsername().isBlank())
            throw new ValidationException("username", "Username is required.");
        if (u.getFullName() == null || u.getFullName().isBlank())
            throw new ValidationException("fullName", "Full name is required.");
        if (u.getRole() == null || (!u.getRole().equals("ADMIN") && !u.getRole().equals("STAFF")))
            throw new ValidationException("role", "Role must be ADMIN or STAFF.");
    }
}
