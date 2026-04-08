package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.dao.ActivityLogDAO;
import com.bofalgan.pharmacy.dao.SupplierDAO;
import com.bofalgan.pharmacy.model.ActivityLog;
import com.bofalgan.pharmacy.model.Supplier;
import com.bofalgan.pharmacy.storage.FileStorageManager;
import com.bofalgan.pharmacy.util.ValidationException;

import java.util.List;

public class SupplierService {

    private final SupplierDAO    supplierDAO;
    private final ActivityLogDAO logDAO;
    private final FileStorageManager fileStorage;
    private final SessionManager session;

    public SupplierService(SupplierDAO supplierDAO, ActivityLogDAO logDAO) {
        this.supplierDAO = supplierDAO;
        this.logDAO      = logDAO;
        this.fileStorage = FileStorageManager.getInstance();
        this.session     = SessionManager.getInstance();
    }

    public Supplier addSupplier(Supplier s) {
        validate(s);
        int id = supplierDAO.insert(s);
        s.setId(id);
        fileStorage.upsertRecord(AppConfig.SUPPLIERS_FILE, s, id);
        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(), session.getCurrentUser().getUsername(),
            "CREATE", "SUPPLIER", id, s.getName(), "Added supplier: " + s.getName()
        ));
        return s;
    }

    public void updateSupplier(Supplier s) {
        validate(s);
        supplierDAO.update(s);
        fileStorage.upsertRecord(AppConfig.SUPPLIERS_FILE, s, s.getId());
        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(), session.getCurrentUser().getUsername(),
            "UPDATE", "SUPPLIER", s.getId(), s.getName(), "Updated supplier: " + s.getName()
        ));
    }

    public void deactivateSupplier(int id) {
        Supplier s = supplierDAO.findById(id);
        if (s == null) throw new ValidationException("Supplier not found.");
        supplierDAO.setActive(id, false);
    }

    public List<Supplier> getAllSuppliers()        { return supplierDAO.findAll(); }
    public List<Supplier> getActiveSuppliers()     { return supplierDAO.findAllActive(); }
    public Supplier getSupplierById(int id)        { return supplierDAO.findById(id); }
    public List<Supplier> searchSuppliers(String q){ return supplierDAO.search(q); }

    private void validate(Supplier s) {
        if (s.getName() == null || s.getName().isBlank())
            throw new ValidationException("name", "Supplier name is required.");
    }
}
