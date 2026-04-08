package com.bofalgan.pharmacy.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class Medicine {
    private int id;
    private String name;
    private String genericName;
    private String category;
    private String strength;
    private String unit;
    private String batchNumber;
    private int quantity;
    private int reorderLevel;
    private double purchasePrice;
    private double sellingPrice;
    private int supplierId;
    private String supplierName; // denormalized for display
    private LocalDate expiryDate;
    private String storageLocation;
    private boolean isPrescriptionOnly;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String barcode;

    public Medicine() {
        this.reorderLevel = 10;
        this.quantity = 0;
        this.isDeleted = false;
        this.isPrescriptionOnly = false;
    }

    // ==================== Expiry helpers ====================
    public long getDaysToExpiry() {
        if (expiryDate == null) return Long.MAX_VALUE;
        return ChronoUnit.DAYS.between(LocalDate.now(), expiryDate);
    }

    public String getExpiryStatus() {
        long days = getDaysToExpiry();
        if (days < 0)  return "EXPIRED";
        if (days <= 7) return "DISCARD";
        if (days <= 30) return "CLEARANCE SALE";
        return "NORMAL STOCK";
    }

    public String getExpiryColorCode() {
        long days = getDaysToExpiry();
        if (days < 0 || days <= 30) return "RED";
        if (days <= 90) return "YELLOW";
        return "GREEN";
    }

    public boolean isLowStock() {
        return quantity <= reorderLevel;
    }

    public double getMarginPercent() {
        if (purchasePrice == 0) return 0;
        return ((sellingPrice - purchasePrice) / purchasePrice) * 100;
    }

    // ==================== Getters and Setters ====================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getGenericName() { return genericName; }
    public void setGenericName(String genericName) { this.genericName = genericName; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getStrength() { return strength; }
    public void setStrength(String strength) { this.strength = strength; }

    public String getUnit() { return unit; }
    public void setUnit(String unit) { this.unit = unit; }

    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public int getReorderLevel() { return reorderLevel; }
    public void setReorderLevel(int reorderLevel) { this.reorderLevel = reorderLevel; }

    public double getPurchasePrice() { return purchasePrice; }
    public void setPurchasePrice(double purchasePrice) { this.purchasePrice = purchasePrice; }

    public double getSellingPrice() { return sellingPrice; }
    public void setSellingPrice(double sellingPrice) { this.sellingPrice = sellingPrice; }

    public int getSupplierId() { return supplierId; }
    public void setSupplierId(int supplierId) { this.supplierId = supplierId; }

    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }

    public LocalDate getExpiryDate() { return expiryDate; }
    public void setExpiryDate(LocalDate expiryDate) { this.expiryDate = expiryDate; }

    public String getStorageLocation() { return storageLocation; }
    public void setStorageLocation(String storageLocation) { this.storageLocation = storageLocation; }

    public boolean isPrescriptionOnly() { return isPrescriptionOnly; }
    public void setPrescriptionOnly(boolean prescriptionOnly) { isPrescriptionOnly = prescriptionOnly; }

    public boolean isDeleted() { return isDeleted; }
    public void setDeleted(boolean deleted) { isDeleted = deleted; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }

    public String getBarcode() { return barcode; }
    public void setBarcode(String barcode) { this.barcode = barcode; }

    @Override
    public String toString() {
        return "Medicine{id=" + id + ", name='" + name + "', qty=" + quantity + "}";
    }
}
