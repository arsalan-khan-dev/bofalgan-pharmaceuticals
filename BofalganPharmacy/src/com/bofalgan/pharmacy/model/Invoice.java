package com.bofalgan.pharmacy.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class Invoice {
    private int id;
    private String invoiceNumber;
    private String customerName;
    private String customerPhone;
    private String customerEmail;
    private double subtotal;
    private String discountType; // "PERCENTAGE", "FIXED", "NONE"
    private double discountValue;
    private double taxAmount;
    private double totalAmount;
    private double paidAmount;
    private String paymentMethod; // "CASH", "CARD", "CREDIT", "CHEQUE"
    private String paymentStatus; // "PAID", "PARTIAL", "DUE"
    private String notes;
    private int createdByUserId;
    private String createdByName;
    private LocalDateTime createdAt;
    private List<InvoiceItem> items;

    public Invoice() {
        this.items = new ArrayList<>();
        this.discountType  = "NONE";
        this.discountValue = 0;
        this.taxAmount     = 0;
        this.paymentStatus = "PAID";
        this.paymentMethod = "CASH";
    }

    public double getChangeAmount() {
        return paidAmount - totalAmount;
    }

    public double getDiscountAmount() {
        if ("PERCENTAGE".equals(discountType)) {
            return subtotal * discountValue / 100;
        } else if ("FIXED".equals(discountType)) {
            return discountValue;
        }
        return 0;
    }

    // ==================== Getters/Setters ====================
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerPhone() { return customerPhone; }
    public void setCustomerPhone(String customerPhone) { this.customerPhone = customerPhone; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public String getDiscountType() { return discountType; }
    public void setDiscountType(String discountType) { this.discountType = discountType; }

    public double getDiscountValue() { return discountValue; }
    public void setDiscountValue(double discountValue) { this.discountValue = discountValue; }

    public double getTaxAmount() { return taxAmount; }
    public void setTaxAmount(double taxAmount) { this.taxAmount = taxAmount; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public double getPaidAmount() { return paidAmount; }
    public void setPaidAmount(double paidAmount) { this.paidAmount = paidAmount; }

    public String getPaymentMethod() { return paymentMethod; }
    public void setPaymentMethod(String paymentMethod) { this.paymentMethod = paymentMethod; }

    public String getPaymentStatus() { return paymentStatus; }
    public void setPaymentStatus(String paymentStatus) { this.paymentStatus = paymentStatus; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public int getCreatedByUserId() { return createdByUserId; }
    public void setCreatedByUserId(int createdByUserId) { this.createdByUserId = createdByUserId; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public List<InvoiceItem> getItems() { return items; }
    public void setItems(List<InvoiceItem> items) { this.items = items; }

    public void addItem(InvoiceItem item) { this.items.add(item); }

    @Override
    public String toString() {
        return "Invoice{" + invoiceNumber + ", total=" + totalAmount + "}";
    }
}
