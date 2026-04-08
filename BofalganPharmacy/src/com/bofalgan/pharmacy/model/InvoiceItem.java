package com.bofalgan.pharmacy.model;

public class InvoiceItem {
    private int id;
    private int invoiceId;
    private int medicineId;
    private String medicineName;
    private String batchNumber;
    private int quantity;
    private double unitPrice;
    private double lineTotal;

    public InvoiceItem() {}

    public InvoiceItem(int medicineId, String medicineName, String batchNumber,
                       int quantity, double unitPrice) {
        this.medicineId   = medicineId;
        this.medicineName = medicineName;
        this.batchNumber  = batchNumber;
        this.quantity     = quantity;
        this.unitPrice    = unitPrice;
        this.lineTotal    = quantity * unitPrice;
    }

    public void recalculate() {
        this.lineTotal = quantity * unitPrice;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getInvoiceId() { return invoiceId; }
    public void setInvoiceId(int invoiceId) { this.invoiceId = invoiceId; }

    public int getMedicineId() { return medicineId; }
    public void setMedicineId(int medicineId) { this.medicineId = medicineId; }

    public String getMedicineName() { return medicineName; }
    public void setMedicineName(String medicineName) { this.medicineName = medicineName; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) {
        this.quantity = quantity;
        this.lineTotal = quantity * unitPrice;
    }

    public double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
        this.lineTotal = quantity * unitPrice;
    }

    public double getLineTotal() { return lineTotal; }
    public void setLineTotal(double lineTotal) { this.lineTotal = lineTotal; }
}
