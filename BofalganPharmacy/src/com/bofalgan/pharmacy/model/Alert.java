package com.bofalgan.pharmacy.model;

import java.time.LocalDateTime;

public class Alert {
    private int id;
    private String alertType; // "LOW_STOCK", "EXPIRY_WARNING", "EXPIRY_CRITICAL", "SYSTEM"
    private String severity;  // "INFO", "WARNING", "CRITICAL"
    private String entityType;
    private int entityId;
    private String entityName;
    private String message;
    private boolean isDismissed;
    private LocalDateTime dismissedAt;
    private int dismissedByUserId;
    private LocalDateTime createdAt;

    public Alert() {}

    public Alert(String alertType, String severity, String entityType,
                 int entityId, String entityName, String message) {
        this.alertType  = alertType;
        this.severity   = severity;
        this.entityType = entityType;
        this.entityId   = entityId;
        this.entityName = entityName;
        this.message    = message;
        this.isDismissed = false;
        this.createdAt  = LocalDateTime.now();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getAlertType() { return alertType; }
    public void setAlertType(String alertType) { this.alertType = alertType; }
    public String getSeverity() { return severity; }
    public void setSeverity(String severity) { this.severity = severity; }
    public String getEntityType() { return entityType; }
    public void setEntityType(String entityType) { this.entityType = entityType; }
    public int getEntityId() { return entityId; }
    public void setEntityId(int entityId) { this.entityId = entityId; }
    public String getEntityName() { return entityName; }
    public void setEntityName(String entityName) { this.entityName = entityName; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    public boolean isDismissed() { return isDismissed; }
    public void setDismissed(boolean dismissed) { isDismissed = dismissed; }
    public LocalDateTime getDismissedAt() { return dismissedAt; }
    public void setDismissedAt(LocalDateTime dismissedAt) { this.dismissedAt = dismissedAt; }
    public int getDismissedByUserId() { return dismissedByUserId; }
    public void setDismissedByUserId(int dismissedByUserId) { this.dismissedByUserId = dismissedByUserId; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
