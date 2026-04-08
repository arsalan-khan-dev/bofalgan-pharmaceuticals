package com.bofalgan.pharmacy.model;

import java.time.LocalDateTime;

public class ActivityLog {
    private int id;
    private int userId;
    private String username;
    private String action;     // "CREATE","UPDATE","DELETE","VIEW","EXPORT","PRINT","LOGIN","LOGOUT"
    private String entityType; // "MEDICINE","INVOICE","PURCHASE","USER","SUPPLIER"
    private int entityId;
    private String entityName;
    private String oldValue;
    private String newValue;
    private String changesSummary;
    private String ipAddress;
    private LocalDateTime timestamp;

    public ActivityLog() {}

    public ActivityLog(int userId, String username, String action,
                       String entityType, int entityId, String entityName,
                       String changesSummary) {
        this.userId         = userId;
        this.username       = username;
        this.action         = action;
        this.entityType     = entityType;
        this.entityId       = entityId;
        this.entityName     = entityName;
        this.changesSummary = changesSummary;
        this.timestamp      = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getOldValue() { return oldValue; }
    public void setOldValue(String oldValue) { this.oldValue = oldValue; }
    public String getNewValue() { return newValue; }
    public void setNewValue(String newValue) { this.newValue = newValue; }
    public String getChangesSummary() { return changesSummary; }
    public void setChangesSummary(String changesSummary) { this.changesSummary = changesSummary; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}
