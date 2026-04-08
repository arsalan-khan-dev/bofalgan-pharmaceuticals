package com.bofalgan.pharmacy.service;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.dao.ActivityLogDAO;
import com.bofalgan.pharmacy.dao.InvoiceDAO;
import com.bofalgan.pharmacy.dao.MedicineDAO;
import com.bofalgan.pharmacy.model.ActivityLog;
import com.bofalgan.pharmacy.model.Invoice;
import com.bofalgan.pharmacy.model.InvoiceItem;
import com.bofalgan.pharmacy.model.Medicine;
import com.bofalgan.pharmacy.storage.FileStorageManager;
import com.bofalgan.pharmacy.util.ValidationException;

import java.time.LocalDate;
import java.util.List;

public class InvoiceService {

    private final InvoiceDAO     invoiceDAO;
    private final MedicineDAO    medicineDAO;
    private final ActivityLogDAO logDAO;
    private final FileStorageManager fileStorage;
    private final SessionManager session;

    public InvoiceService(InvoiceDAO invoiceDAO, MedicineDAO medicineDAO, ActivityLogDAO logDAO) {
        this.invoiceDAO   = invoiceDAO;
        this.medicineDAO  = medicineDAO;
        this.logDAO       = logDAO;
        this.fileStorage  = FileStorageManager.getInstance();
        this.session      = SessionManager.getInstance();
    }

    public Invoice createInvoice(Invoice invoice) {
        validateInvoice(invoice);
        invoice.setCreatedByUserId(session.getCurrentUser().getId());

        // Verify stock availability
        for (InvoiceItem item : invoice.getItems()) {
            Medicine med = medicineDAO.findById(item.getMedicineId());
            if (med == null)
                throw new ValidationException("Medicine not found: ID " + item.getMedicineId());
            if (med.getQuantity() < item.getQuantity())
                throw new ValidationException("Insufficient stock for " + med.getName() +
                    ". Available: " + med.getQuantity() + ", Requested: " + item.getQuantity());
        }

        recalculateTotals(invoice);
        int id = invoiceDAO.create(invoice, medicineDAO);

        // Mirror to file storage
        fileStorage.upsertRecord(AppConfig.INVOICES_FILE, invoice, id);
        fileStorage.logSync("invoices", 1, true, "Invoice " + invoice.getInvoiceNumber());

        logDAO.insert(new ActivityLog(
            session.getCurrentUser().getId(),
            session.getCurrentUser().getUsername(),
            "CREATE", "INVOICE", id, invoice.getInvoiceNumber(),
            "Invoice " + invoice.getInvoiceNumber() + " for $" + String.format("%.2f", invoice.getTotalAmount())
        ));

        return invoice;
    }

    public Invoice getInvoiceById(int id) {
        return invoiceDAO.findById(id);
    }

    public List<Invoice> getAllInvoices(int limit, int offset) {
        return invoiceDAO.findAll(limit, offset);
    }

    public List<Invoice> getInvoicesByDateRange(LocalDate start, LocalDate end) {
        return invoiceDAO.findByDateRange(start, end);
    }

    public int getTotalInvoiceCount()   { return invoiceDAO.countTotal(); }
    public double getTodayRevenue()     { return invoiceDAO.getTodayRevenue(); }
    public double getMonthRevenue()     { return invoiceDAO.getMonthRevenue(); }

    private void validateInvoice(Invoice inv) {
        if (inv.getItems() == null || inv.getItems().isEmpty())
            throw new ValidationException("Invoice must have at least one item.");
        for (InvoiceItem item : inv.getItems()) {
            if (item.getQuantity() <= 0)
                throw new ValidationException("Item quantity must be greater than 0.");
        }
    }

    public void recalculateTotals(Invoice inv) {
        double subtotal = 0;
        for (InvoiceItem item : inv.getItems()) {
            item.recalculate();
            subtotal += item.getLineTotal();
        }
        inv.setSubtotal(subtotal);
        double discount = inv.getDiscountAmount();
        double total = subtotal - discount + inv.getTaxAmount();
        inv.setTotalAmount(Math.max(0, total));
        if (inv.getPaidAmount() >= inv.getTotalAmount()) {
            inv.setPaymentStatus("PAID");
        } else if (inv.getPaidAmount() > 0) {
            inv.setPaymentStatus("PARTIAL");
        } else {
            inv.setPaymentStatus("DUE");
        }
    }
}
