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
import javafx.stage.*;

import java.time.LocalDate;
import java.util.List;

// =====================================================================
//  PurchaseController
// =====================================================================
public class PurchaseController {
    private final AppContext ctx; private final MainController main;
    private TableView<Purchase> table; private ObservableList<Purchase> items;

    public PurchaseController(AppContext ctx, MainController main) { this.ctx=ctx; this.main=main; }

    public Node buildView() {
        VBox view = new VBox(0); view.setStyle("-fx-background-color:#F8FAFC;");
        view.getChildren().addAll(buildToolbar(), buildContent());
        loadPurchases(); return view;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14,24,14,24));
        bar.setStyle("-fx-background-color:white;-fx-border-color:"+AppConfig.COLOR_BORDER+";-fx-border-width:0 0 1 0;");
        Label title = UIFactory.createH2("Purchase Orders");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = UIFactory.createButton("+ New Purchase", UIFactory.ButtonType.PRIMARY);
        addBtn.setOnAction(e -> showPurchaseForm());
        bar.getChildren().addAll(title, spacer, addBtn); return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(0); content.setPadding(new Insets(16,24,16,24));
        VBox.setVgrow(content, Priority.ALWAYS);
        table = UIFactory.createStyledTable(); items = FXCollections.observableArrayList(); table.setItems(items);
        addCol("PO #",     80, p -> String.valueOf(p.getId()));
        addCol("Supplier", 160, p -> p.getSupplierName() != null ? p.getSupplierName() : "");
        addCol("Date",     100, p -> p.getPurchaseDate() != null ? DateUtils.format(p.getPurchaseDate()) : "");
        addCol("Items",     60, p -> String.valueOf(p.getItems().size()));
        addCol("Total",     100, p -> CurrencyFormatter.format(p.getTotalAmount()));
        addCol("Paid",      100, p -> CurrencyFormatter.format(p.getPaidAmount()));
        addCol("Status",    100, p -> p.getPaymentStatus() != null ? p.getPaymentStatus() : "");
        VBox.setVgrow(table, Priority.ALWAYS); content.getChildren().add(table); return content;
    }

    private void addCol(String h, double w, java.util.function.Function<Purchase,String> fn) {
        TableColumn<Purchase,String> col = new TableColumn<>(h); col.setPrefWidth(w);
        col.setCellValueFactory(d -> new SimpleStringProperty(fn.apply(d.getValue())));
        table.getColumns().add(col);
    }

    private void loadPurchases() {
        Thread t = new Thread(() -> {
            List<Purchase> data = ctx.getPurchaseService().getAllPurchases();
            Platform.runLater(() -> items.setAll(data));
        }); t.setDaemon(true); t.start();
    }

    private void showPurchaseForm() {
        Stage dialog = new Stage(); dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("New Purchase Order"); dialog.setMinWidth(700); dialog.setMinHeight(600);

        VBox form = new VBox(14); form.setPadding(new Insets(24)); form.setStyle("-fx-background-color:white;");
        form.getChildren().add(UIFactory.createH2("New Purchase Order"));

        ComboBox<String> supplierCombo = UIFactory.createComboBox();
        List<Supplier> suppliers = ctx.getSupplierService().getActiveSuppliers();
        suppliers.forEach(s -> supplierCombo.getItems().add(s.getId() + " | " + s.getName()));
        DatePicker datePicker = new DatePicker(LocalDate.now());

        VBox cartBox = new VBox(8);
        ObservableList<PurchaseItem> cartItems = FXCollections.observableArrayList();
        TableView<PurchaseItem> cartTable = UIFactory.createStyledTable(); cartTable.setItems(cartItems);
        TableColumn<PurchaseItem,String> medCol = new TableColumn<>("Medicine"); medCol.setPrefWidth(180);
        medCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));
        TableColumn<PurchaseItem,String> batchCol2 = new TableColumn<>("Batch"); batchCol2.setPrefWidth(100);
        batchCol2.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatchNumber()));
        TableColumn<PurchaseItem,String> qtyCol2 = new TableColumn<>("Qty"); qtyCol2.setPrefWidth(70);
        qtyCol2.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantityAccepted())));
        TableColumn<PurchaseItem,String> priceCol2 = new TableColumn<>("Unit Price"); priceCol2.setPrefWidth(90);
        priceCol2.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.formatNoSymbol(d.getValue().getUnitPrice())));
        TableColumn<PurchaseItem,String> totalCol2 = new TableColumn<>("Total"); totalCol2.setPrefWidth(90);
        totalCol2.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.format(d.getValue().getLineTotal())));
        cartTable.getColumns().addAll(medCol, batchCol2, qtyCol2, priceCol2, totalCol2);
        cartTable.setPrefHeight(200);

        // Add item row
        ComboBox<String> medCombo = UIFactory.createComboBox();
        ctx.getMedicineService().getAllMedicines().forEach(m -> medCombo.getItems().add(m.getId()+" | "+m.getName()));
        TextField batchF = UIFactory.createTextField("Batch #"); TextField qtyF = UIFactory.createTextField("Qty");
        TextField priceF = UIFactory.createTextField("Unit Price"); DatePicker expF = new DatePicker();
        expF.setPromptText("Expiry"); expF.setPrefHeight(UIConstants.INPUT_HEIGHT);
        Button addItemBtn = UIFactory.createButton("Add Item", UIFactory.ButtonType.SUCCESS, UIFactory.ButtonSize.SMALL);
        addItemBtn.setOnAction(e -> {
            try {
                String medVal = medCombo.getValue();
                if (medVal == null) return;
                int medId = Integer.parseInt(medVal.split("\\|")[0].trim());
                String medName = medVal.split("\\|")[1].trim();
                int qty = Integer.parseInt(qtyF.getText().trim());
                double price = Double.parseDouble(priceF.getText().trim());
                cartItems.add(new PurchaseItem(medId, medName, batchF.getText().trim(), qty, price, expF.getValue()));
            } catch (Exception ex) { UIFactory.showErrorDialog("Invalid Input", ex.getMessage()); }
        });

        HBox addRow = new HBox(8, medCombo, batchF, qtyF, priceF, expF, addItemBtn);
        addRow.setAlignment(Pos.CENTER_LEFT);
        medCombo.setMaxWidth(220); batchF.setPrefWidth(90); qtyF.setPrefWidth(60);
        priceF.setPrefWidth(80); expF.setPrefWidth(120);

        ComboBox<String> paymentStatusCombo = UIFactory.createComboBox();
        paymentStatusCombo.getItems().addAll("PENDING","PARTIAL","PAID"); paymentStatusCombo.setValue("PENDING");
        TextField paidAmtField = UIFactory.createTextField("0.00"); paidAmtField.setPrefWidth(120);
        HBox payRow = new HBox(10, new Label("Payment:"), paymentStatusCombo, new Label("Paid Amount:"), paidAmtField);
        payRow.setAlignment(Pos.CENTER_LEFT);

        Label errLbl = new Label(""); errLbl.setTextFill(Color.web(AppConfig.COLOR_ERROR)); errLbl.setVisible(false);
        Button saveBtn = UIFactory.createButton("Create Purchase Order", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.LARGE);
        Button cancelBtn = UIFactory.createButton("Cancel", UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
        HBox btns = new HBox(10, cancelBtn, saveBtn); btns.setAlignment(Pos.CENTER_RIGHT);
        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> {
            try {
                if (cartItems.isEmpty()) throw new ValidationException("Add at least one item.");
                String supVal = supplierCombo.getValue();
                if (supVal == null) throw new ValidationException("Select a supplier.");
                Purchase p = new Purchase();
                p.setSupplierId(Integer.parseInt(supVal.split("\\|")[0].trim()));
                p.setPurchaseDate(datePicker.getValue());
                p.setPaymentStatus(paymentStatusCombo.getValue());
                try { p.setPaidAmount(Double.parseDouble(paidAmtField.getText())); } catch (Exception ignored) {}
                p.setItems(cartItems);
                ctx.getPurchaseService().createPurchaseOrder(p);
                dialog.close(); loadPurchases();
            } catch (Exception ex) { errLbl.setText("⚠ "+ex.getMessage()); errLbl.setVisible(true); }
        });

        form.getChildren().addAll(
            UIFactory.createFormField("Supplier *", supplierCombo),
            UIFactory.createFormField("Purchase Date", datePicker),
            new Label("Add Items:"), addRow, cartTable, payRow, errLbl, btns
        );
        supplierCombo.setMaxWidth(Double.MAX_VALUE); datePicker.setMaxWidth(Double.MAX_VALUE);
        ScrollPane sp = new ScrollPane(form); sp.setFitToWidth(true); sp.setStyle("-fx-background:white;");
        dialog.setScene(new javafx.scene.Scene(sp)); dialog.showAndWait();
    }
}
