package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.model.Invoice;
import com.bofalgan.pharmacy.model.InvoiceItem;
import com.bofalgan.pharmacy.ui.UIFactory;
import com.bofalgan.pharmacy.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

public class InvoiceController {

    private final AppContext     ctx;
    private final MainController main;
    private TableView<Invoice>   table;
    private ObservableList<Invoice> items;

    public InvoiceController(AppContext ctx, MainController main) { this.ctx=ctx; this.main=main; }

    public Node buildView() {
        VBox view = new VBox(0); view.setStyle("-fx-background-color:#F8FAFC;");
        view.getChildren().addAll(buildToolbar(), buildContent());
        loadInvoices(); return view;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14,24,14,24));
        bar.setStyle("-fx-background-color:white;-fx-border-color:"+AppConfig.COLOR_BORDER+";-fx-border-width:0 0 1 0;");
        Label title = UIFactory.createH2("Invoice History");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button exportBtn = UIFactory.createButton("Export Excel", UIFactory.ButtonType.SECONDARY);
        exportBtn.setOnAction(e -> handleExportExcel());
        bar.getChildren().addAll(title, spacer, exportBtn); return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(0); content.setPadding(new Insets(16,24,16,24));
        VBox.setVgrow(content, Priority.ALWAYS);
        table = UIFactory.createStyledTable(); items = FXCollections.observableArrayList(); table.setItems(items);
        addCol("Invoice #", 120, i -> i.getInvoiceNumber());
        addCol("Date",       110, i -> i.getCreatedAt() != null ? DateUtils.format(i.getCreatedAt()) : "");
        addCol("Customer",   130, i -> i.getCustomerName() != null ? i.getCustomerName() : "Walk-in");
        addCol("Phone",       100, i -> i.getCustomerPhone() != null ? i.getCustomerPhone() : "");
        addCol("Total",        100, i -> CurrencyFormatter.format(i.getTotalAmount()));
        addCol("Paid",         100, i -> CurrencyFormatter.format(i.getPaidAmount()));
        addCol("Method",        80, i -> i.getPaymentMethod() != null ? i.getPaymentMethod() : "");
        addCol("Status",        80, i -> i.getPaymentStatus() != null ? i.getPaymentStatus() : "");
        addCol("By",           100, i -> i.getCreatedByName() != null ? i.getCreatedByName() : "");

        TableColumn<Invoice, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(120);
        actCol.setCellFactory(col -> new TableCell<Invoice, Void>() {
            final Button viewBtn   = UIFactory.createButton("View", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.SMALL);
            final Button printBtn  = UIFactory.createButton("PDF",  UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.SMALL);
            final HBox pane = new HBox(4, viewBtn, printBtn);
            { pane.setAlignment(Pos.CENTER);
              viewBtn.setOnAction(e  -> showInvoiceDetail(getTableView().getItems().get(getIndex())));
              printBtn.setOnAction(e -> handlePrintPDF(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : pane); }
        });
        table.getColumns().add(actCol);

        // Row color by payment status
        table.setRowFactory(tv -> new TableRow<Invoice>() {
            @Override protected void updateItem(Invoice inv, boolean empty) {
                super.updateItem(inv, empty);
                if (empty || inv == null) { setStyle(""); return; }
                if ("DUE".equals(inv.getPaymentStatus())) setStyle("-fx-background-color:#FFF8E1;");
                else setStyle(getIndex() % 2 == 0 ? "" : "-fx-background-color:"+AppConfig.COLOR_ROW_ALT+";");
            }
        });

        VBox.setVgrow(table, Priority.ALWAYS); content.getChildren().add(table); return content;
    }

    private void addCol(String h, double w, java.util.function.Function<Invoice,String> fn) {
        TableColumn<Invoice,String> col = new TableColumn<>(h); col.setPrefWidth(w);
        col.setCellValueFactory(d -> new SimpleStringProperty(fn.apply(d.getValue())));
        table.getColumns().add(col);
    }

    private void loadInvoices() {
        Thread t = new Thread(() -> {
            List<Invoice> data = ctx.getInvoiceService().getAllInvoices(200, 0);
            // Load items for each (for display)
            data.forEach(inv -> inv.setItems(ctx.getInvoiceDAO().findItemsByInvoiceId(inv.getId())));
            Platform.runLater(() -> items.setAll(data));
        }); t.setDaemon(true); t.start();
    }

    private void showInvoiceDetail(Invoice inv) {
        javafx.stage.Stage dialog = new javafx.stage.Stage();
        dialog.initModality(javafx.stage.Modality.APPLICATION_MODAL);
        dialog.setTitle("Invoice: " + inv.getInvoiceNumber());
        VBox form = new VBox(10); form.setPadding(new Insets(20)); form.setStyle("-fx-background-color:white;");

        form.getChildren().add(UIFactory.createH2("Invoice " + inv.getInvoiceNumber()));
        form.getChildren().add(UIFactory.createSmall("Date: " + DateUtils.format(inv.getCreatedAt()) +
            "  |  Customer: " + (inv.getCustomerName() != null ? inv.getCustomerName() : "Walk-in"), null));

        TableView<InvoiceItem> itemTable = UIFactory.createStyledTable();
        ObservableList<InvoiceItem> itemsObs = FXCollections.observableArrayList(
            ctx.getInvoiceDAO().findItemsByInvoiceId(inv.getId()));
        itemTable.setItems(itemsObs); itemTable.setPrefHeight(220);

        TableColumn<InvoiceItem,String> medCol = new TableColumn<>("Medicine"); medCol.setPrefWidth(200);
        medCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        TableColumn<InvoiceItem,String> qtyCol = new TableColumn<>("Qty"); qtyCol.setPrefWidth(60);
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantity())));
        TableColumn<InvoiceItem,String> priceCol = new TableColumn<>("Price"); priceCol.setPrefWidth(90);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.format(d.getValue().getUnitPrice())));
        TableColumn<InvoiceItem,String> totalCol = new TableColumn<>("Total"); totalCol.setPrefWidth(90);
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.format(d.getValue().getLineTotal())));
        itemTable.getColumns().addAll(medCol, qtyCol, priceCol, totalCol);

        Label subtotalLbl = new Label("Subtotal: " + CurrencyFormatter.format(inv.getSubtotal()));
        Label totalLbl    = new Label("TOTAL: " + CurrencyFormatter.format(inv.getTotalAmount()));
        totalLbl.setStyle("-fx-font-size:16;-fx-font-weight:bold;");
        Label paidLbl  = new Label("Paid (" + inv.getPaymentMethod() + "): " + CurrencyFormatter.format(inv.getPaidAmount()));
        Label changeLbl = new Label("Change: " + CurrencyFormatter.format(Math.max(0, inv.getChangeAmount())));

        Button pdfBtn = UIFactory.createButton("Export PDF", UIFactory.ButtonType.PRIMARY);
        pdfBtn.setOnAction(e -> { handlePrintPDF(inv); dialog.close(); });
        Button closeBtn = UIFactory.createButton("Close", UIFactory.ButtonType.SECONDARY);
        closeBtn.setOnAction(e -> dialog.close());
        HBox btns = new HBox(10, closeBtn, pdfBtn); btns.setAlignment(Pos.CENTER_RIGHT);

        form.getChildren().addAll(itemTable, new Separator(), subtotalLbl, totalLbl, paidLbl, changeLbl, btns);
        dialog.setScene(new javafx.scene.Scene(form, 520, 520)); dialog.showAndWait();
    }

    private void handlePrintPDF(Invoice inv) {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Invoice PDF"); fc.setInitialFileName(inv.getInvoiceNumber() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF","*.pdf"));
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            // Load items if not yet loaded
            if (inv.getItems() == null || inv.getItems().isEmpty())
                inv.setItems(ctx.getInvoiceDAO().findItemsByInvoiceId(inv.getId()));
            PDFExporter.generateInvoicePDF(inv, f.getAbsolutePath());
            UIFactory.showInfoDialog("PDF Saved", "Invoice saved to:\n" + f.getAbsolutePath());
        } catch (IOException ex) {
            UIFactory.showErrorDialog("PDF Error", ex.getMessage());
        }
    }

    private void handleExportExcel() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Excel"); fc.setInitialFileName("invoices_" + LocalDate.now() + ".xlsx");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel","*.xlsx"));
        File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            ExcelExporter.exportInvoices(items, f.getAbsolutePath());
            UIFactory.showInfoDialog("Export Complete", "Saved to:\n" + f.getAbsolutePath());
        } catch (Exception ex) { UIFactory.showErrorDialog("Export Error", ex.getMessage()); }
    }
}
