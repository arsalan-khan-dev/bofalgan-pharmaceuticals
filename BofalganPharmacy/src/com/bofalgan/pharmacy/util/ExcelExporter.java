package com.bofalgan.pharmacy.util;

import com.bofalgan.pharmacy.model.Invoice;
import com.bofalgan.pharmacy.model.Medicine;
import com.bofalgan.pharmacy.model.Purchase;
import com.bofalgan.pharmacy.model.Supplier;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ExcelExporter {

    // ==================== MEDICINES ====================

    public static void exportMedicines(List<Medicine> medicines, String filePath) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Medicines");

            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle altStyle    = createAltRowStyle(wb);
            CellStyle normalStyle = createNormalStyle(wb);

            // Title row
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("Bofalgan Pharmaceuticals - Medicine Inventory");
            CellStyle titleStyle = wb.createCellStyle();
            Font titleFont = wb.createFont();
            titleFont.setBold(true); titleFont.setFontHeightInPoints((short)14);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 10));

            // Sub-title
            Row subRow = sheet.createRow(1);
            subRow.createCell(0).setCellValue("Generated: " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm")));
            sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 10));

            // Header row
            String[] headers = {"#","Name","Generic Name","Category","Unit","Batch","Qty","Reorder Lvl","Purchase Price","Selling Price","Expiry Date","Days Left","Status"};
            Row headerRow = sheet.createRow(3);
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 4;
            for (int i = 0; i < medicines.size(); i++) {
                Medicine m = medicines.get(i);
                Row row = sheet.createRow(rowNum++);
                CellStyle style = (i % 2 == 0) ? normalStyle : altStyle;

                setCellValue(row, 0, i + 1, style);
                setCellValue(row, 1, m.getName(), style);
                setCellValue(row, 2, m.getGenericName() != null ? m.getGenericName() : "", style);
                setCellValue(row, 3, m.getCategory() != null ? m.getCategory() : "", style);
                setCellValue(row, 4, m.getUnit() != null ? m.getUnit() : "", style);
                setCellValue(row, 5, m.getBatchNumber(), style);
                setCellValue(row, 6, m.getQuantity(), style);
                setCellValue(row, 7, m.getReorderLevel(), style);
                setCellValue(row, 8, m.getPurchasePrice(), style);
                setCellValue(row, 9, m.getSellingPrice(), style);
                setCellValue(row, 10, m.getExpiryDate() != null ? DateUtils.format(m.getExpiryDate()) : "", style);
                setCellValue(row, 11, (int) m.getDaysToExpiry(), style);
                setCellValue(row, 12, m.getExpiryStatus(), style);
            }

            // Auto-size columns
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 6000) sheet.setColumnWidth(i, 6000);
            }

            // Totals row
            Row totalRow = sheet.createRow(rowNum + 1);
            CellStyle boldStyle = wb.createCellStyle();
            Font bold = wb.createFont(); bold.setBold(true);
            boldStyle.setFont(bold);
            Cell totalLabel = totalRow.createCell(0);
            totalLabel.setCellValue("Total Medicines: " + medicines.size());
            totalLabel.setCellStyle(boldStyle);

            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                wb.write(fos);
            }
        }
    }

    // ==================== INVOICES ====================

    public static void exportInvoices(List<Invoice> invoices, String filePath) throws IOException {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Sales Report");
            CellStyle headerStyle = createHeaderStyle(wb);
            CellStyle altStyle    = createAltRowStyle(wb);
            CellStyle normalStyle = createNormalStyle(wb);

            Row headerRow = sheet.createRow(0);
            String[] headers = {"Invoice #","Date","Customer","Phone","Items","Subtotal","Discount","Tax","Total","Paid","Method","Status"};
            for (int i = 0; i < headers.length; i++) {
                Cell c = headerRow.createCell(i);
                c.setCellValue(headers[i]);
                c.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            double grandTotal = 0;
            for (int i = 0; i < invoices.size(); i++) {
                Invoice inv = invoices.get(i);
                Row row = sheet.createRow(rowNum++);
                CellStyle style = (i % 2 == 0) ? normalStyle : altStyle;
                setCellValue(row, 0, inv.getInvoiceNumber(), style);
                setCellValue(row, 1, inv.getCreatedAt() != null ? DateUtils.format(inv.getCreatedAt()) : "", style);
                setCellValue(row, 2, inv.getCustomerName() != null ? inv.getCustomerName() : "Walk-in", style);
                setCellValue(row, 3, inv.getCustomerPhone() != null ? inv.getCustomerPhone() : "", style);
                setCellValue(row, 4, inv.getItems() != null ? inv.getItems().size() : 0, style);
                setCellValue(row, 5, inv.getSubtotal(), style);
                setCellValue(row, 6, inv.getDiscountAmount(), style);
                setCellValue(row, 7, inv.getTaxAmount(), style);
                setCellValue(row, 8, inv.getTotalAmount(), style);
                setCellValue(row, 9, inv.getPaidAmount(), style);
                setCellValue(row, 10, inv.getPaymentMethod(), style);
                setCellValue(row, 11, inv.getPaymentStatus(), style);
                grandTotal += inv.getTotalAmount();
            }

            Row totalRow = sheet.createRow(rowNum + 1);
            CellStyle boldStyle = wb.createCellStyle();
            Font bold = wb.createFont(); bold.setBold(true);
            boldStyle.setFont(bold);
            totalRow.createCell(7).setCellValue("GRAND TOTAL:");
            Cell totCell = totalRow.createCell(8);
            totCell.setCellValue(grandTotal);
            totCell.setCellStyle(boldStyle);

            for (int i = 0; i < headers.length; i++) sheet.autoSizeColumn(i);

            try (FileOutputStream fos = new FileOutputStream(filePath)) { wb.write(fos); }
        }
    }

    // ==================== STYLE HELPERS ====================

    private static CellStyle createHeaderStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        Font font = wb.createFont();
        font.setBold(true); font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setBorderBottom(BorderStyle.THIN);
        return style;
    }

    private static CellStyle createNormalStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private static CellStyle createAltRowStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setBorderBottom(BorderStyle.HAIR);
        return style;
    }

    private static void setCellValue(Row row, int col, Object value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value instanceof String)  cell.setCellValue((String) value);
        else if (value instanceof Integer) cell.setCellValue((Integer) value);
        else if (value instanceof Double)  cell.setCellValue((Double) value);
        else if (value instanceof Long)    cell.setCellValue((Long) value);
        else cell.setCellValue(value != null ? value.toString() : "");
        cell.setCellStyle(style);
    }

    private ExcelExporter() {}
}
