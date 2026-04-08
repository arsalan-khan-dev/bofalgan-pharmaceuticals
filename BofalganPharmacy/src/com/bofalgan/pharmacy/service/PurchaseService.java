package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.dao.ActivityLogDAO;
import com.bofalgan.pharmacy.dao.MedicineDAO;
import com.bofalgan.pharmacy.dao.PurchaseDAO;
import com.bofalgan.pharmacy.dao.SupplierDAO;
import com.bofalgan.pharmacy.dao.UserDAO;
import com.bofalgan.pharmacy.model.*;
import com.bofalgan.pharmacy.storage.FileStorageManager;
import com.bofalgan.pharmacy.util.ValidationException;

import java.time.LocalDate;
import java.util.List;

public class PurchaseService {

    private final PurchaseDAO    purchaseDAO;
    private final MedicineDAO    medicineDAO;
    private final ActivityLogDAO logDAO;
    private final FileStorageManager fileStorage;
    private final SessionManager session;

    public PurchaseService(PurchaseDAO purchaseDAO, MedicineDAO medicineDAO, ActivityLogDAO logDAO) {
        this.purchaseDAO = purchaseDAO;
        this.medicineDAO = medicineDAO;
        this.logDAO      = logDAO;
        this.fileStorage = FileStorageManager.getInstance();
        this.session     = SessionManager.getInstance();
    }

    public Purchase createPurchaseOrder(Purchase purchase) {
        if (!session.isAdmin()) throw new ValidationException("Only admins can create purchase orders.");
        if (purchase.getItems() == null || purchase.getItems().isEmpty())
            throw new ValidationException("Purchase order must have at least one item.");
        if (purchase.getSupplierId() <= 0)
            throw new ValidationException("Supplier is required.");

        double total = 0;
        for (PurchaseItem item : purchase.getItems()) {
            if (item.getQuantityAccepted() <= 0)
                throw new ValidationException("Item quantity must be greater than 0.");
            total += item.getLineTotal();
        }
        total = total + purchase.getTaxAmount() - purchase.getDiscountAmount();
        purchase.setTotalAmount(Math.max(0, total));
        purchase.setCreatedByUserId(session.getCurrentUser().getId());

        int id = purchaseDAO.create(purchase, medicineDAO);
        fileStorage.upsertRecord(AppConfig.PURCHASES_FILE, purchase, id);
        fileStorage.logSync("purchases", 1, true, "PO #" + id);

        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "CREATE", "PURCHASE", id, "PO #" + id,
            "Purchase order created for $" + String.format("%.2f", purchase.getTotalAmount())
        ));
        return purchase;
    }

    public Purchase getPurchaseById(int id)     { return purchaseDAO.findById(id); }
    public List<Purchase> getAllPurchases()      { return purchaseDAO.findAll(); }
    public List<Purchase> getPurchasesByDateRange(LocalDate s, LocalDate e) {
        return purchaseDAO.findByDateRange(s, e);
    }

    public void updatePaymentStatus(int purchaseId, String status, double paidAmount) {
        if (!session.isAdmin()) throw new ValidationException("Only admins can update payment status.");
        purchaseDAO.updatePaymentStatus(purchaseId, status, paidAmount);
    }
}
