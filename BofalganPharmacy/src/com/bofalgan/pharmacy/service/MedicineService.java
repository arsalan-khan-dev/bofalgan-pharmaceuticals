package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.dao.ActivityLogDAO;
import com.bofalgan.pharmacy.dao.AlertDAO;
import com.bofalgan.pharmacy.dao.MedicineDAO;
import com.bofalgan.pharmacy.model.ActivityLog;
import com.bofalgan.pharmacy.model.Alert;
import com.bofalgan.pharmacy.model.Medicine;
import com.bofalgan.pharmacy.storage.FileStorageManager;
import com.bofalgan.pharmacy.util.ValidationException;

import java.time.LocalDate;
import java.util.List;

public class MedicineService {

    private final MedicineDAO     medicineDAO;
    private final AlertDAO        alertDAO;
    private final ActivityLogDAO  logDAO;
    private final FileStorageManager fileStorage;
    private final SessionManager  session;

    public MedicineService(MedicineDAO medicineDAO, AlertDAO alertDAO, ActivityLogDAO logDAO) {
        this.medicineDAO  = medicineDAO;
        this.alertDAO     = alertDAO;
        this.logDAO       = logDAO;
        this.fileStorage  = FileStorageManager.getInstance();
        this.session      = SessionManager.getInstance();
    }

    // ==================== ADD ====================

    public Medicine addMedicine(Medicine m) {
        validate(m);
        int id = medicineDAO.insert(m);
        m.setId(id);

        // Mirror to file storage
        fileStorage.upsertRecord(AppConfig.MEDICINES_FILE, m, id);
        fileStorage.logSync("medicines", 1, true, "Added: " + m.getName());

        // Audit log
        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "CREATE", "MEDICINE", id, m.getName(),
            "Added medicine: " + m.getName() + " (Qty: " + m.getQuantity() + ")"
        ));

        // Check alerts for new medicine
        checkAndCreateAlerts(m);
        return m;
    }

    // ==================== UPDATE ====================

    public void updateMedicine(Medicine updated) {
        validate(updated);
        Medicine existing = medicineDAO.findById(updated.getId());
        if (existing == null) throw new ValidationException("Medicine not found.");

        medicineDAO.update(updated);

        // Mirror to file storage
        fileStorage.upsertRecord(AppConfig.MEDICINES_FILE, updated, updated.getId());
        fileStorage.logSync("medicines", 1, true, "Updated: " + updated.getName());

        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "UPDATE", "MEDICINE", updated.getId(), updated.getName(),
            "Updated medicine. Qty: " + existing.getQuantity() + " -> " + updated.getQuantity()
        ));

        checkAndCreateAlerts(updated);
    }

    // ==================== DELETE ====================

    public void deleteMedicine(int id) {
        if (!session.isAdmin()) throw new ValidationException("Only admins can delete medicines.");
        Medicine m = medicineDAO.findById(id);
        if (m == null) throw new ValidationException("Medicine not found.");

        medicineDAO.delete(id);
        fileStorage.softDelete(AppConfig.MEDICINES_FILE, id);
        fileStorage.logSync("medicines", 1, true, "Deleted: " + m.getName());

        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "DELETE", "MEDICINE", id, m.getName(),
            "Soft-deleted medicine: " + m.getName()
        ));

        alertDAO.clearEntityAlerts("MEDICINE", id);
    }

    // ==================== QUERIES (delegate to DAO) ====================

    public List<Medicine> getAllMedicines() {
        return medicineDAO.findAll();
    }

    public Medicine getMedicineById(int id) {
        return medicineDAO.findById(id);
    }

    public Medicine getMedicineByBarcode(String barcode) {
        return medicineDAO.findByBarcode(barcode);
    }

    public List<Medicine> searchMedicines(String query) {
        if (query == null || query.isBlank()) return medicineDAO.findAll();
        return medicineDAO.search(query);
    }

    public List<Medicine> getMedicinesByCategory(String category) {
        return medicineDAO.findByCategory(category);
    }

    public List<Medicine> getMedicinesNearExpiry(int days) {
        return medicineDAO.findExpiringSoon(days);
    }

    public List<Medicine> getMedicinesBelowReorderLevel() {
        return medicineDAO.findBelowReorderLevel();
    }

    public List<String> getAllCategories() {
        return medicineDAO.getAllCategories();
    }

    // ==================== DASHBOARD STATS ====================

    public int getTotalMedicines()              { return medicineDAO.countAll(); }
    public int getLowStockCount()               { return medicineDAO.countLowStock(); }
    public int getExpiringCount(int days)       { return medicineDAO.countExpiringSoon(days); }
    public double getTotalInventoryValue()      { return medicineDAO.getTotalInventoryValue(); }

    // ==================== ALERTS ====================

    public void runAlertAudit() {
        List<Medicine> expiring = medicineDAO.findExpiringSoon(AppConfig.EXPIRY_WARNING_DAYS);
        for (Medicine m : expiring) {
            long days = m.getDaysToExpiry();
            String type, severity;
            if (days <= AppConfig.EXPIRY_CRITICAL_DAYS) {
                type = "EXPIRY_CRITICAL"; severity = "CRITICAL";
            } else {
                type = "EXPIRY_WARNING"; severity = "WARNING";
            }
            Alert a = new Alert(type, severity, "MEDICINE", m.getId(), m.getName(),
                m.getName() + " expires in " + days + " day(s) (Batch: " + m.getBatchNumber() + ")");
            alertDAO.clearEntityAlerts("MEDICINE", m.getId());
            alertDAO.insert(a);
        }

        List<Medicine> lowStock = medicineDAO.findBelowReorderLevel();
        for (Medicine m : lowStock) {
            Alert a = new Alert("LOW_STOCK", "WARNING", "MEDICINE", m.getId(), m.getName(),
                m.getName() + " is low on stock. Qty: " + m.getQuantity() + " (Reorder: " + m.getReorderLevel() + ")");
            alertDAO.clearEntityAlerts("MEDICINE", m.getId());
            alertDAO.insert(a);
        }
    }

    private void checkAndCreateAlerts(Medicine m) {
        if (m.isLowStock()) {
            alertDAO.clearEntityAlerts("MEDICINE", m.getId());
            alertDAO.insert(new Alert("LOW_STOCK", "WARNING", "MEDICINE",
                m.getId(), m.getName(),
                m.getName() + " is below reorder level. Qty: " + m.getQuantity()));
        }
        long days = m.getDaysToExpiry();
        if (days <= AppConfig.EXPIRY_CRITICAL_DAYS) {
            alertDAO.clearEntityAlerts("MEDICINE", m.getId());
            alertDAO.insert(new Alert("EXPIRY_CRITICAL", "CRITICAL", "MEDICINE",
                m.getId(), m.getName(),
                m.getName() + " expires in " + days + " day(s)."));
        } else if (days <= AppConfig.EXPIRY_WARNING_DAYS) {
            alertDAO.clearEntityAlerts("MEDICINE", m.getId());
            alertDAO.insert(new Alert("EXPIRY_WARNING", "WARNING", "MEDICINE",
                m.getId(), m.getName(),
                m.getName() + " expires in " + days + " day(s)."));
        }
    }

    // ==================== VALIDATION ====================

    private void validate(Medicine m) {
        if (m.getName() == null || m.getName().isBlank())
            throw new ValidationException("name", "Medicine name is required.");
        if (m.getBatchNumber() == null || m.getBatchNumber().isBlank())
            throw new ValidationException("batchNumber", "Batch number is required.");
        if (m.getQuantity() < 0)
            throw new ValidationException("quantity", "Quantity cannot be negative.");
        if (m.getPurchasePrice() <= 0)
            throw new ValidationException("purchasePrice", "Purchase price must be greater than 0.");
        if (m.getSellingPrice() <= 0)
            throw new ValidationException("sellingPrice", "Selling price must be greater than 0.");
        if (m.getSellingPrice() < m.getPurchasePrice())
            throw new ValidationException("sellingPrice", "Selling price should not be less than purchase price.");
        if (m.getExpiryDate() == null)
            throw new ValidationException("expiryDate", "Expiry date is required.");
        if (m.getUnit() == null || m.getUnit().isBlank())
            throw new ValidationException("unit", "Unit is required.");
    }
}
