package com.bofalgan.pharmacy.model;

import java.time.LocalDate;

public class PurchaseItem {
    private int id;
    private int purchaseId;
    private int medicineId;
    private String medicineName;
    private String batchNumber;
    private int quantityReceived;
    private int quantityAccepted;
    private double unitPrice;
    private LocalDate expiryDate;
    private String notes;

    public PurchaseItem() {}

    public PurchaseItem(int medicineId, String medicineName, String batchNumber,
                        int quantityReceived, double unitPrice, LocalDate expiryDate) {
        this.medicineId       = medicineId;
        this.medicineName     = medicineName;
        this.batchNumber      = batchNumber;
        this.quantityReceived = quantityReceived;
        this.quantityAccepted = quantityReceived;
        this.unitPrice        = unitPrice;
        this.expiryDate       = expiryDate;
    }

    public double getLineTotal() { return quantityAccepted * unitPrice; }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getPurchaseId() { return purchaseId; }
    public void setPurchaseId(int purchaseId) { this.purchaseId = purchaseId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public int getQuantityReceived() { return quantityReceived; }
    public void setQuantityReceived(int quantityReceived) { this.quantityReceived = quantityReceived; }

    public int getQuantityAccepted() { return quantityAccepted; }
    public void setQuantityAccepted(int quantityAccepted) { this.quantityAccepted = quantityAccepted; }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) { this.unitPrice = unitPrice; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
}
