package com.bofalgan.pharmacy.util;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.model.Invoice;
import com.bofalgan.pharmacy.model.InvoiceItem;
import com.bofalgan.pharmacy.model.Medicine;
import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.*;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class PDFExporter {

    // ==================== INVOICE PDF ====================

    public static void generateInvoicePDF(Invoice inv, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDFont fontBold  = PDType1Font.HELVETICA_BOLD;
            PDFont fontPlain = PDType1Font.HELVETICA;
            PDFont fontMono  = PDType1Font.COURIER;

            float margin = 50;
            float pageWidth  = page.getMediaBox().getWidth();
            float yStart     = page.getMediaBox().getHeight() - margin;
            float[] y = {yStart};

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                // Header
                drawText(cs, fontBold, 18, margin, y[0], AppConfig.APP_NAME);
                y[0] -= 20;
                drawText(cs, fontPlain, 10, margin, y[0], "Professional Pharmacy Management System");
                y[0] -= 30;

                // Separator
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 20;

                // Invoice details
                drawText(cs, fontBold, 13, margin, y[0], "INVOICE");
                drawText(cs, fontPlain, 10, pageWidth - 200, y[0], "Invoice #: " + inv.getInvoiceNumber());
                y[0] -= 16;
                drawText(cs, fontPlain, 10, pageWidth - 200, y[0], "Date: " + DateUtils.format(inv.getCreatedAt()));
                y[0] -= 16;
                drawText(cs, fontPlain, 10, pageWidth - 200, y[0], "Time: " + (inv.getCreatedAt() != null ? inv.getCreatedAt().toLocalTime().toString().substring(0,5) : ""));
                y[0] -= 10;

                if (inv.getCustomerName() != null && !inv.getCustomerName().isBlank()) {
                    drawText(cs, fontPlain, 10, margin, y[0], "Customer: " + inv.getCustomerName());
                    y[0] -= 15;
                }
                if (inv.getCustomerPhone() != null && !inv.getCustomerPhone().isBlank()) {
                    drawText(cs, fontPlain, 10, margin, y[0], "Phone: " + inv.getCustomerPhone());
                    y[0] -= 15;
                }

                y[0] -= 10;
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 18;

                // Column headers
                float col1 = margin, col2 = 200, col3 = 310, col4 = 380, col5 = 450;
                drawText(cs, fontBold, 10, col1, y[0], "#");
                drawText(cs, fontBold, 10, col1 + 15, y[0], "Medicine");
                drawText(cs, fontBold, 10, col2 + 30, y[0], "Batch");
                drawText(cs, fontBold, 10, col3 + 10, y[0], "Qty");
                drawText(cs, fontBold, 10, col4, y[0], "Price");
                drawText(cs, fontBold, 10, col5, y[0], "Total");
                y[0] -= 5;
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 16;

                // Items
                if (inv.getItems() != null) {
                    int lineNum = 1;
                    for (InvoiceItem item : inv.getItems()) {
                        drawText(cs, fontPlain, 9, col1, y[0], String.valueOf(lineNum++));
                        String name = item.getMedicineName() != null ? item.getMedicineName() : "Medicine";
                        if (name.length() > 22) name = name.substring(0, 22);
                        drawText(cs, fontPlain, 9, col1 + 15, y[0], name);
                        drawText(cs, fontMono, 9, col2 + 30, y[0], item.getBatchNumber() != null ? item.getBatchNumber() : "");
                        drawText(cs, fontPlain, 9, col3 + 10, y[0], String.valueOf(item.getQuantity()));
                        drawText(cs, fontPlain, 9, col4, y[0], CurrencyFormatter.formatNoSymbol(item.getUnitPrice()));
                        drawText(cs, fontPlain, 9, col5, y[0], CurrencyFormatter.formatNoSymbol(item.getLineTotal()));
                        y[0] -= 15;
                    }
                }

                y[0] -= 5;
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 18;

                // Totals
                float labelX = col5 - 80;
                drawText(cs, fontPlain, 10, labelX, y[0], "Subtotal:");
                drawText(cs, fontPlain, 10, col5, y[0], CurrencyFormatter.format(inv.getSubtotal()));
                y[0] -= 15;

                if (!"NONE".equals(inv.getDiscountType()) && inv.getDiscountValue() > 0) {
                    drawText(cs, fontPlain, 10, labelX, y[0], "Discount:");
                    drawText(cs, fontPlain, 10, col5, y[0], "- " + CurrencyFormatter.format(inv.getDiscountAmount()));
                    y[0] -= 15;
                }

                if (inv.getTaxAmount() > 0) {
                    drawText(cs, fontPlain, 10, labelX, y[0], "Tax:");
                    drawText(cs, fontPlain, 10, col5, y[0], CurrencyFormatter.format(inv.getTaxAmount()));
                    y[0] -= 15;
                }

                drawLine(cs, labelX, pageWidth - margin, y[0]);
                y[0] -= 16;
                drawText(cs, fontBold, 12, labelX, y[0], "TOTAL:");
                drawText(cs, fontBold, 12, col5, y[0], CurrencyFormatter.format(inv.getTotalAmount()));
                y[0] -= 20;

                drawText(cs, fontPlain, 10, labelX, y[0], "Paid (" + inv.getPaymentMethod() + "):");
                drawText(cs, fontPlain, 10, col5, y[0], CurrencyFormatter.format(inv.getPaidAmount()));
                y[0] -= 15;

                if (inv.getChangeAmount() > 0) {
                    drawText(cs, fontPlain, 10, labelX, y[0], "Change:");
                    drawText(cs, fontPlain, 10, col5, y[0], CurrencyFormatter.format(inv.getChangeAmount()));
                    y[0] -= 15;
                }

                y[0] -= 30;
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 20;
                drawText(cs, fontPlain, 9, margin, y[0], "Thank you for your purchase!");
                y[0] -= 14;
                drawText(cs, fontPlain, 8, margin, y[0], "Printed by: " + (inv.getCreatedByName() != null ? inv.getCreatedByName() : "Staff") + "  |  " + AppConfig.APP_NAME);
            }

            doc.save(filePath);
        }
    }

    // ==================== INVENTORY PDF ====================

    public static void generateInventoryReportPDF(List<Medicine> medicines, String filePath) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4.landscape() != null ? new PDRectangle(842, 595) : PDRectangle.A4);
            doc.addPage(page);

            PDFont fontBold  = PDType1Font.HELVETICA_BOLD;
            PDFont fontPlain = PDType1Font.HELVETICA;

            float margin = 40;
            float pageWidth = page.getMediaBox().getWidth();
            float[] y = {page.getMediaBox().getHeight() - margin};

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                drawText(cs, fontBold, 14, margin, y[0], "Bofalgan Pharmaceuticals - Inventory Report");
                y[0] -= 16;
                drawText(cs, fontPlain, 9, margin, y[0], "Generated: " + DateUtils.format(java.time.LocalDateTime.now()) + "  Total: " + medicines.size() + " medicines");
                y[0] -= 8;
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 14;

                float[] cols = {margin, margin+20, margin+160, margin+260, margin+310, margin+360, margin+420, margin+480, margin+540};
                String[] headers = {"#","Name","Category","Qty","Reorder","Sell Price","Expiry","Status"};
                for (int i = 0; i < headers.length; i++) {
                    drawText(cs, fontBold, 8, cols[i], y[0], headers[i]);
                }
                y[0] -= 5;
                drawLine(cs, margin, pageWidth - margin, y[0]);
                y[0] -= 12;

                int num = 1;
                for (Medicine m : medicines) {
                    if (y[0] < 60) break; // Simple single-page for now
                    drawText(cs, fontPlain, 7, cols[0], y[0], String.valueOf(num++));
                    String name = m.getName().length() > 20 ? m.getName().substring(0,20) : m.getName();
                    drawText(cs, fontPlain, 7, cols[1], y[0], name);
                    drawText(cs, fontPlain, 7, cols[2], y[0], m.getCategory() != null ? m.getCategory() : "");
                    drawText(cs, fontPlain, 7, cols[3], y[0], String.valueOf(m.getQuantity()));
                    drawText(cs, fontPlain, 7, cols[4], y[0], String.valueOf(m.getReorderLevel()));
                    drawText(cs, fontPlain, 7, cols[5], y[0], CurrencyFormatter.formatNoSymbol(m.getSellingPrice()));
                    drawText(cs, fontPlain, 7, cols[6], y[0], m.getExpiryDate() != null ? DateUtils.format(m.getExpiryDate()) : "");
                    drawText(cs, fontPlain, 7, cols[7], y[0], m.getExpiryStatus());
                    y[0] -= 12;
                }
            }
            doc.save(filePath);
        }
    }

    // ==================== HELPERS ====================

    private static void drawText(PDPageContentStream cs, PDFont font, int size, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? text : "");
        cs.endText();
    }

    private static void drawLine(PDPageContentStream cs, float x1, float x2, float y) throws IOException {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    private PDFExporter() {}
}
