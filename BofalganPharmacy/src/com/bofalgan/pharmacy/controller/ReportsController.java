package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.model.*;
import com.bofalgan.pharmacy.service.SessionManager;
import com.bofalgan.pharmacy.ui.UIFactory;
import com.bofalgan.pharmacy.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.File;
import java.sql.Connection;
import java.time.LocalDate;
import java.util.List;

// =====================================================================
//  ReportsController
// =====================================================================
public class ReportsController {
    private final AppContext ctx; private final MainController main;
    public ReportsController(AppContext ctx, MainController main) { this.ctx=ctx; this.main=main; }

    public Node buildView() {
        VBox view = new VBox(0); view.setStyle("-fx-background-color:#F8FAFC;");
        HBox toolbar = new HBox(10); toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(14,24,14,24));
        toolbar.setStyle("-fx-background-color:white;-fx-border-color:"+AppConfig.COLOR_BORDER+";-fx-border-width:0 0 1 0;");
        toolbar.getChildren().add(UIFactory.createH2("Reports"));
        view.getChildren().add(toolbar);

        VBox content = new VBox(16); content.setPadding(new Insets(24));
        VBox.setVgrow(content, Priority.ALWAYS);

        // Report type buttons
        String[] reports = {"Inventory Report","Sales Report","Purchase Report",
            "Expiry Clearance Report","Audit Log","Profit Margin Report (Admin)"};
        FlowPane btnsPane = new FlowPane(12, 12);

        for (String rpt : reports) {
            Button btn = UIFactory.createButton(rpt, UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
            if (rpt.contains("Admin") && !SessionManager.getInstance().isAdmin()) {
                btn.setDisable(true);
            }
            btn.setOnAction(e -> generateReport(rpt));
            btnsPane.getChildren().add(btn);
        }

        Label hint = new Label("Select a report type to generate. Reports can be exported to Excel or PDF.");
        hint.setFont(UIConstants.body()); hint.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));

        content.getChildren().addAll(UIFactory.createSectionHeader("Available Reports"), hint, btnsPane);
        view.getChildren().add(content); return view;
    }

    private void generateReport(String type) {
        Stage dialog = new Stage(); dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(type); dialog.setMinWidth(880); dialog.setMinHeight(600);

        VBox form = new VBox(12); form.setPadding(new Insets(20)); form.setStyle("-fx-background-color:white;");
        form.getChildren().add(UIFactory.createH2(type));

        // Date range filter
        DatePicker startPicker = new DatePicker(LocalDate.now().minusDays(30));
        DatePicker endPicker   = new DatePicker(LocalDate.now());
        HBox dateRow = new HBox(10, new Label("From:"), startPicker, new Label("To:"), endPicker);
        dateRow.setAlignment(Pos.CENTER_LEFT);

        TableView<String[]> reportTable = new TableView<>();
        ObservableList<String[]> reportData = FXCollections.observableArrayList();
        reportTable.setItems(reportData);
        VBox.setVgrow(reportTable, Priority.ALWAYS);

        Button generateBtn = UIFactory.createButton("Generate Report", UIFactory.ButtonType.PRIMARY);
        Button exportBtn   = UIFactory.createButton("Export Excel", UIFactory.ButtonType.SECONDARY);
        Button closeBtn    = UIFactory.createButton("Close", UIFactory.ButtonType.SECONDARY);
        HBox btns = new HBox(10, closeBtn, exportBtn, generateBtn); btns.setAlignment(Pos.CENTER_RIGHT);
        closeBtn.setOnAction(e -> dialog.close());

        Label statusLbl = new Label("Click 'Generate Report' to load data.");
        statusLbl.setFont(UIConstants.small()); statusLbl.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));

        generateBtn.setOnAction(e -> {
            reportTable.getColumns().clear(); reportData.clear();
            statusLbl.setText("Loading...");
            Thread t = new Thread(() -> buildReportData(type, startPicker.getValue(), endPicker.getValue(),
                reportTable, reportData, statusLbl));
            t.setDaemon(true); t.start();
        });

        exportBtn.setOnAction(e -> {
            if (reportData.isEmpty()) { UIFactory.showInfoDialog("No Data","Generate report first."); return; }
            javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
            fc.setInitialFileName(type.replace(" ","_")+"_"+LocalDate.now()+".xlsx");
            fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel","*.xlsx"));
            File f = fc.showSaveDialog(null);
            if (f == null) return;
            try {
                exportReportExcel(reportTable, reportData, type, f.getAbsolutePath());
                UIFactory.showInfoDialog("Exported","Saved to: "+f.getAbsolutePath());
            } catch (Exception ex) { UIFactory.showErrorDialog("Export Error",ex.getMessage()); }
        });

        form.getChildren().addAll(dateRow, reportTable, statusLbl, btns);
        dialog.setScene(new javafx.scene.Scene(form)); dialog.showAndWait();
    }

    private void buildReportData(String type, LocalDate start, LocalDate end,
                                  TableView<String[]> table, ObservableList<String[]> data, Label statusLbl) {
        try {
            List<String[]> rows;
            String[] headers;
            switch (type) {
                case "Inventory Report" -> {
                    headers = new String[]{"Name","Generic","Category","Batch","Qty","Reorder","Sell Price","Expiry","Days Left","Status"};
                    rows = ctx.getMedicineService().getAllMedicines().stream().map(m -> new String[]{
                        m.getName(), m.getGenericName()!=null?m.getGenericName():"",
                        m.getCategory()!=null?m.getCategory():"", m.getBatchNumber(),
                        String.valueOf(m.getQuantity()), String.valueOf(m.getReorderLevel()),
                        CurrencyFormatter.format(m.getSellingPrice()),
                        m.getExpiryDate()!=null?DateUtils.format(m.getExpiryDate()):"",
                        String.valueOf(m.getDaysToExpiry()), m.getExpiryStatus()
                    }).toList();
                }
                case "Sales Report" -> {
                    headers = new String[]{"Invoice #","Date","Customer","Total","Paid","Method","Status"};
                    rows = ctx.getInvoiceService().getInvoicesByDateRange(start, end).stream().map(i -> new String[]{
                        i.getInvoiceNumber(), DateUtils.format(i.getCreatedAt()),
                        i.getCustomerName()!=null?i.getCustomerName():"Walk-in",
                        CurrencyFormatter.format(i.getTotalAmount()), CurrencyFormatter.format(i.getPaidAmount()),
                        i.getPaymentMethod()!=null?i.getPaymentMethod():"", i.getPaymentStatus()
                    }).toList();
                }
                case "Purchase Report" -> {
                    headers = new String[]{"PO #","Supplier","Date","Total","Paid","Status"};
                    rows = ctx.getPurchaseService().getPurchasesByDateRange(start, end).stream().map(p -> new String[]{
                        String.valueOf(p.getId()), p.getSupplierName()!=null?p.getSupplierName():"",
                        DateUtils.format(p.getPurchaseDate()), CurrencyFormatter.format(p.getTotalAmount()),
                        CurrencyFormatter.format(p.getPaidAmount()), p.getPaymentStatus()
                    }).toList();
                }
                case "Expiry Clearance Report" -> {
                    headers = new String[]{"Name","Batch","Qty","Expiry","Days Left","Sell Price","Status"};
                    rows = ctx.getMedicineService().getMedicinesNearExpiry(30).stream().map(m -> new String[]{
                        m.getName(), m.getBatchNumber(), String.valueOf(m.getQuantity()),
                        DateUtils.format(m.getExpiryDate()), String.valueOf(m.getDaysToExpiry()),
                        CurrencyFormatter.format(m.getSellingPrice()), m.getExpiryStatus()
                    }).toList();
                }
                case "Profit Margin Report (Admin)" -> {
                    if (!SessionManager.getInstance().isAdmin()) return;
                    headers = new String[]{"Medicine","Qty Sold","Purchase $","Sell $","Margin %","Total Profit"};
                    rows = ctx.getAnalyticsDAO().getProfitByMedicine(start, end, 50).stream().map(r -> new String[]{
                        (String)r[0], String.valueOf(r[1]),
                        CurrencyFormatter.format((double)r[2]), CurrencyFormatter.format((double)r[3]),
                        String.format("%.1f%%", (double)r[4]), CurrencyFormatter.format((double)r[5])
                    }).toList();
                }
                case "Audit Log" -> {
                    headers = new String[]{"Timestamp","User","Action","Entity","Details"};
                    rows = ctx.getActivityLogDAO().findAll(500).stream().map(l -> new String[]{
                        DateUtils.format(l.getTimestamp()), l.getUsername()!=null?l.getUsername():"",
                        l.getAction(), l.getEntityType()!=null?l.getEntityType():"",
                        l.getChangesSummary()!=null?l.getChangesSummary():""
                    }).toList();
                }
                default -> { headers = new String[]{}; rows = List.of(); }
            }
            final String[] finalHeaders = headers;
            final List<String[]> finalRows = rows;
            Platform.runLater(() -> {
                table.getColumns().clear();
                for (int i = 0; i < finalHeaders.length; i++) {
                    final int idx = i;
                    TableColumn<String[], String> col = new TableColumn<>(finalHeaders[i]);
                    col.setCellValueFactory(d -> new SimpleStringProperty(idx < d.getValue().length ? d.getValue()[idx] : ""));
                    table.getColumns().add(col);
                }
                data.setAll(finalRows);
                statusLbl.setText("Showing " + finalRows.size() + " records.");
            });
        } catch (Exception ex) {
            Platform.runLater(() -> statusLbl.setText("Error: " + ex.getMessage()));
        }
    }

    private void exportReportExcel(TableView<String[]> table, ObservableList<String[]> data,
                                    String title, String path) throws Exception {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook wb = new org.apache.poi.xssf.usermodel.XSSFWorkbook()) {
            org.apache.poi.ss.usermodel.Sheet sheet = wb.createSheet(title.substring(0, Math.min(title.length(), 30)));
            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            for (int i = 0; i < table.getColumns().size(); i++) {
                headerRow.createCell(i).setCellValue(table.getColumns().get(i).getText());
            }
            for (int r = 0; r < data.size(); r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r+1);
                String[] rowData = data.get(r);
                for (int c = 0; c < rowData.length; c++) row.createCell(c).setCellValue(rowData[c]);
            }
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(path)) { wb.write(fos); }
        }
    }
}
