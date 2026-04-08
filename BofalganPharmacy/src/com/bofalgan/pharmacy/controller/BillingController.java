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

import java.util.List;

public class BillingController {

    private final AppContext     ctx;
    private final MainController main;

    // Cart state
    private ObservableList<InvoiceItem> cart = FXCollections.observableArrayList();
    private List<Medicine>              allMedicines;

    // UI refs
    private TextField   searchField, customerNameField, customerPhoneField, amountTenderedField;
    private ComboBox<String> discountTypeCombo, paymentMethodCombo;
    private TextField   discountValueField;
    private Label       subtotalLabel, discountLabel, taxLabel, totalLabel, changeLabel;
    private TableView<InvoiceItem> cartTable;
    private Label       cartStatusLabel;
    private ListView<Medicine> medicineListView;

    public BillingController(AppContext ctx, MainController main) {
        this.ctx  = ctx;
        this.main = main;
    }

    public Node buildView() {
        VBox view = new VBox(0);
        view.setStyle("-fx-background-color: #F8FAFC;");

        view.getChildren().addAll(buildToolbar(), buildPOSArea());
        loadMedicines();
        return view;
    }

    // ==================== TOOLBAR ====================

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;");

        Label title = UIFactory.createH2("Billing / Point of Sale");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);

        Button clearBtn = UIFactory.createButton("Clear Cart", UIFactory.ButtonType.DANGER);
        clearBtn.setOnAction(e -> clearCart());
        bar.getChildren().addAll(title, spacer, clearBtn);
        return bar;
    }

    // ==================== MAIN POS AREA ====================

    private HBox buildPOSArea() {
        HBox pos = new HBox(0);
        VBox.setVgrow(pos, Priority.ALWAYS);

        // Left: medicine search + list
        VBox left = buildMedicinePanel();
        left.setPrefWidth(380);
        left.setMinWidth(320);

        // Right: cart + checkout
        VBox right = buildCartPanel();
        HBox.setHgrow(right, Priority.ALWAYS);

        Separator sep = new Separator(javafx.geometry.Orientation.VERTICAL);
        pos.getChildren().addAll(left, sep, right);
        return pos;
    }

    // ==================== LEFT: MEDICINE PANEL ====================

    private VBox buildMedicinePanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: white;");

        // Search header
        VBox searchHeader = new VBox(8);
        searchHeader.setPadding(new Insets(12));
        searchHeader.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;");

        Label searchLbl = new Label("Search Medicines");
        searchLbl.setFont(UIConstants.bodyBold());

        searchField = UIFactory.createTextField("Type name or scan barcode...");
        searchField.setMaxWidth(Double.MAX_VALUE);
        searchField.textProperty().addListener((obs, old, val) -> filterMedicineList(val));

        // Barcode listener: auto-add on Enter
        searchField.setOnAction(e -> {
            String barcode = searchField.getText().trim();
            Medicine med = ctx.getMedicineService().getMedicineByBarcode(barcode);
            if (med != null) {
                addToCart(med, 1);
                searchField.clear();
            }
        });

        searchHeader.getChildren().addAll(searchLbl, searchField);

        // Medicine list
        medicineListView = new ListView<>();
        medicineListView.setStyle("-fx-background-color: white;");
        VBox.setVgrow(medicineListView, Priority.ALWAYS);

        medicineListView.setCellFactory(lv -> new ListCell<Medicine>() {
            @Override protected void updateItem(Medicine m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setGraphic(null); return; }

                HBox cell = new HBox(8);
                cell.setAlignment(Pos.CENTER_LEFT);
                cell.setPadding(new Insets(6, 10, 6, 10));

                VBox info = new VBox(2);
                Label nameLbl = new Label(m.getName());
                nameLbl.setFont(UIConstants.bodyBold());
                Label detailLbl = new Label("Batch: " + m.getBatchNumber() + "  |  Qty: " + m.getQuantity() +
                    "  |  " + CurrencyFormatter.format(m.getSellingPrice()));
                detailLbl.setFont(UIConstants.small());
                detailLbl.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));
                info.getChildren().addAll(nameLbl, detailLbl);

                // Stock color
                if (m.getQuantity() == 0) {
                    nameLbl.setTextFill(Color.web(AppConfig.COLOR_ERROR));
                    detailLbl.setText("OUT OF STOCK");
                } else if (m.isLowStock()) {
                    nameLbl.setTextFill(Color.web(AppConfig.COLOR_WARNING));
                }

                Button addBtn = UIFactory.createButton("+", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.SMALL);
                addBtn.setDisable(m.getQuantity() == 0);
                addBtn.setOnAction(e -> promptQuantityAndAdd(m));

                HBox.setHgrow(info, Priority.ALWAYS);
                cell.getChildren().addAll(info, addBtn);
                setGraphic(cell);
            }
        });

        medicineListView.setOnMouseClicked(e -> {
            if (e.getClickCount() == 2) {
                Medicine selected = medicineListView.getSelectionModel().getSelectedItem();
                if (selected != null && selected.getQuantity() > 0) promptQuantityAndAdd(selected);
            }
        });

        panel.getChildren().addAll(searchHeader, medicineListView);
        return panel;
    }

    // ==================== RIGHT: CART PANEL ====================

    private VBox buildCartPanel() {
        VBox panel = new VBox(0);
        panel.setStyle("-fx-background-color: white;");

        // Cart header
        HBox cartHeader = new HBox(10);
        cartHeader.setAlignment(Pos.CENTER_LEFT);
        cartHeader.setPadding(new Insets(12, 16, 12, 16));
        cartHeader.setStyle("-fx-background-color: #F1F5F9; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;");
        Label cartTitle = new Label("Shopping Cart");
        cartTitle.setFont(UIConstants.h3());
        cartStatusLabel = new Label("0 items");
        cartStatusLabel.setFont(UIConstants.small());
        cartStatusLabel.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));
        HBox.setHgrow(new Region(), Priority.ALWAYS);
        cartHeader.getChildren().addAll(cartTitle, new Region(), cartStatusLabel);
        cartHeader.getChildren().get(1).setStyle(""); HBox.setHgrow(cartHeader.getChildren().get(1), Priority.ALWAYS);

        // Cart table
        cartTable = UIFactory.createStyledTable();
        cartTable.setItems(cart);
        VBox.setVgrow(cartTable, Priority.ALWAYS);

        TableColumn<InvoiceItem, String> nameCol = new TableColumn<>("Medicine");
        nameCol.setPrefWidth(180);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getMedicineName()));

        TableColumn<InvoiceItem, String> batchCol = new TableColumn<>("Batch");
        batchCol.setPrefWidth(80);
        batchCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getBatchNumber()));

        TableColumn<InvoiceItem, String> qtyCol = new TableColumn<>("Qty");
        qtyCol.setPrefWidth(60);
        qtyCol.setCellValueFactory(d -> new SimpleStringProperty(String.valueOf(d.getValue().getQuantity())));
        qtyCol.setEditable(true);

        TableColumn<InvoiceItem, String> priceCol = new TableColumn<>("Price");
        priceCol.setPrefWidth(80);
        priceCol.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.formatNoSymbol(d.getValue().getUnitPrice())));

        TableColumn<InvoiceItem, String> totalCol = new TableColumn<>("Total");
        totalCol.setPrefWidth(90);
        totalCol.setCellValueFactory(d -> new SimpleStringProperty(CurrencyFormatter.format(d.getValue().getLineTotal())));

        TableColumn<InvoiceItem, Void> removeCol = new TableColumn<>("");
        removeCol.setPrefWidth(50);
        removeCol.setCellFactory(col -> new TableCell<InvoiceItem, Void>() {
            final Button btn = UIFactory.createButton("✕", UIFactory.ButtonType.DANGER, UIFactory.ButtonSize.SMALL);
            { btn.setOnAction(e -> { cart.remove(getTableView().getItems().get(getIndex())); recalculate(); }); }
            @Override protected void updateItem(Void v, boolean empty) {
                super.updateItem(v, empty);
                setGraphic(empty ? null : btn);
            }
        });

        cartTable.getColumns().addAll(nameCol, batchCol, qtyCol, priceCol, totalCol, removeCol);

        // Totals panel
        VBox totalsPanel = buildTotalsPanel();

        panel.getChildren().addAll(cartHeader, cartTable, totalsPanel);
        return panel;
    }

    private VBox buildTotalsPanel() {
        VBox panel = new VBox(10);
        panel.setPadding(new Insets(14, 16, 14, 16));
        panel.setStyle("-fx-background-color: #F8FAFC; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 1 0 0 0;");

        // Customer info
        GridPane custGrid = new GridPane();
        custGrid.setHgap(10); custGrid.setVgap(6);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        custGrid.getColumnConstraints().addAll(c1, c2);

        customerNameField  = UIFactory.createTextField("Customer name (optional)");
        customerPhoneField = UIFactory.createTextField("Phone (optional)");
        custGrid.add(UIFactory.createFormField("Customer Name", customerNameField), 0, 0);
        custGrid.add(UIFactory.createFormField("Phone", customerPhoneField), 1, 0);

        // Discount row
        HBox discountRow = new HBox(8);
        discountTypeCombo = UIFactory.createComboBox();
        discountTypeCombo.getItems().addAll("NONE", "PERCENTAGE", "FIXED");
        discountTypeCombo.setValue("NONE");
        discountTypeCombo.setPrefWidth(130);
        discountTypeCombo.setOnAction(e -> recalculate());

        discountValueField = UIFactory.createTextField("0");
        discountValueField.setPrefWidth(80);
        discountValueField.textProperty().addListener((o,ov,nv) -> recalculate());

        discountRow.setAlignment(Pos.CENTER_LEFT);
        discountRow.getChildren().addAll(new Label("Discount:"), discountTypeCombo, discountValueField);

        // Payment method
        paymentMethodCombo = UIFactory.createComboBox();
        paymentMethodCombo.getItems().addAll("CASH","CARD","CREDIT","CHEQUE");
        paymentMethodCombo.setValue("CASH");

        amountTenderedField = UIFactory.createTextField("0.00");
        amountTenderedField.textProperty().addListener((o,ov,nv) -> updateChange());

        HBox payRow = new HBox(10);
        payRow.setAlignment(Pos.CENTER_LEFT);
        payRow.getChildren().addAll(
            new Label("Method:"), paymentMethodCombo,
            new Label("Tendered:"), amountTenderedField
        );

        // Totals display
        GridPane totalsGrid = new GridPane();
        totalsGrid.setHgap(20); totalsGrid.setVgap(4);
        ColumnConstraints tc1 = new ColumnConstraints(); tc1.setHalignment(HPos.RIGHT); tc1.setMinWidth(120);
        ColumnConstraints tc2 = new ColumnConstraints(); tc2.setHalignment(HPos.RIGHT); tc2.setMinWidth(100);
        totalsGrid.getColumnConstraints().addAll(tc1, tc2);

        subtotalLabel = new Label("$0.00"); discountLabel = new Label("$0.00");
        taxLabel = new Label("$0.00");      totalLabel    = new Label("$0.00");
        changeLabel = new Label("$0.00");

        totalLabel.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 18));
        totalLabel.setTextFill(Color.web(AppConfig.COLOR_PRIMARY));

        addTotalRow(totalsGrid, 0, "Subtotal:", subtotalLabel);
        addTotalRow(totalsGrid, 1, "Discount:", discountLabel);
        addTotalRow(totalsGrid, 2, "Tax:",      taxLabel);
        addTotalRow(totalsGrid, 3, "TOTAL:",    totalLabel);
        addTotalRow(totalsGrid, 4, "Change:",   changeLabel);

        // Complete sale button
        Button completeSaleBtn = UIFactory.createButton("✓  Complete Sale", UIFactory.ButtonType.SUCCESS, UIFactory.ButtonSize.LARGE);
        completeSaleBtn.setMaxWidth(Double.MAX_VALUE);
        completeSaleBtn.setOnAction(e -> handleCompleteSale());

        panel.getChildren().addAll(custGrid, discountRow, payRow, new Separator(), totalsGrid, completeSaleBtn);
        return panel;
    }

    private void addTotalRow(GridPane grid, int row, String label, Label valueLabel) {
        Label lbl = new Label(label);
        lbl.setFont(UIConstants.bodyBold());
        grid.add(lbl, 0, row);
        grid.add(valueLabel, 1, row);
    }

    // ==================== CART LOGIC ====================

    private void promptQuantityAndAdd(Medicine m) {
        TextInputDialog dialog = new TextInputDialog("1");
        dialog.setTitle("Add to Cart");
        dialog.setHeaderText(m.getName());
        dialog.setContentText("Quantity (Max: " + m.getQuantity() + "):");
        dialog.showAndWait().ifPresent(val -> {
            try {
                int qty = Integer.parseInt(val.trim());
                if (qty <= 0) { UIFactory.showErrorDialog("Invalid", "Quantity must be > 0"); return; }
                if (qty > m.getQuantity()) { UIFactory.showErrorDialog("Insufficient Stock", "Only " + m.getQuantity() + " available."); return; }
                addToCart(m, qty);
            } catch (NumberFormatException ex) {
                UIFactory.showErrorDialog("Invalid Input", "Please enter a valid number.");
            }
        });
    }

    private void addToCart(Medicine m, int qty) {
        for (InvoiceItem item : cart) {
            if (item.getMedicineId() == m.getId()) {
                item.setQuantity(item.getQuantity() + qty);
                item.recalculate();
                cartTable.refresh();
                recalculate();
                return;
            }
        }
        cart.add(new InvoiceItem(m.getId(), m.getName(), m.getBatchNumber(), qty, m.getSellingPrice()));
        recalculate();
    }

    private void recalculate() {
        double subtotal = 0;
        for (InvoiceItem item : cart) subtotal += item.getLineTotal();

        double discountAmt = 0;
        String discType  = discountTypeCombo.getValue();
        try {
            double discVal = Double.parseDouble(discountValueField.getText().replace(",",""));
            if ("PERCENTAGE".equals(discType)) discountAmt = subtotal * discVal / 100;
            else if ("FIXED".equals(discType))  discountAmt = discVal;
        } catch (Exception ignored) {}

        double total = Math.max(0, subtotal - discountAmt);

        subtotalLabel.setText(CurrencyFormatter.format(subtotal));
        discountLabel.setText("- " + CurrencyFormatter.format(discountAmt));
        taxLabel.setText(CurrencyFormatter.format(0));
        totalLabel.setText(CurrencyFormatter.format(total));
        cartStatusLabel.setText(cart.size() + " item(s)");

        updateChange();
    }

    private void updateChange() {
        try {
            double total   = CurrencyFormatter.parse(totalLabel.getText());
            double tendered = CurrencyFormatter.parse(amountTenderedField.getText());
            double change = tendered - total;
            changeLabel.setText(CurrencyFormatter.format(Math.max(0, change)));
            changeLabel.setTextFill(change >= 0 ? Color.web(AppConfig.COLOR_SUCCESS) : Color.web(AppConfig.COLOR_ERROR));
        } catch (Exception ignored) {}
    }

    private void clearCart() {
        cart.clear();
        recalculate();
        searchField.clear();
        customerNameField.clear();
        customerPhoneField.clear();
        amountTenderedField.setText("0.00");
        discountTypeCombo.setValue("NONE");
        discountValueField.setText("0");
    }

    // ==================== COMPLETE SALE ====================

    private void handleCompleteSale() {
        if (cart.isEmpty()) {
            UIFactory.showErrorDialog("Empty Cart", "Add at least one medicine to the cart.");
            return;
        }
        double total   = CurrencyFormatter.parse(totalLabel.getText());
        double tendered = CurrencyFormatter.parse(amountTenderedField.getText());
        if ("CASH".equals(paymentMethodCombo.getValue()) && tendered < total) {
            UIFactory.showErrorDialog("Insufficient Payment", "Amount tendered is less than total.");
            return;
        }

        boolean confirm = UIFactory.showConfirmDialog("Complete Sale",
            "Confirm sale of " + cart.size() + " item(s) for " + totalLabel.getText() + "?");
        if (!confirm) return;

        try {
            Invoice inv = new Invoice();
            inv.setCustomerName(customerNameField.getText().trim());
            inv.setCustomerPhone(customerPhoneField.getText().trim());
            inv.setDiscountType(discountTypeCombo.getValue());
            try { inv.setDiscountValue(Double.parseDouble(discountValueField.getText())); } catch (Exception ignored) {}
            inv.setPaidAmount(tendered);
            inv.setPaymentMethod(paymentMethodCombo.getValue());
            inv.setItems(cart);
            inv.setCreatedByUserId(SessionManager.getInstance().getCurrentUser().getId());

            ctx.getInvoiceService().recalculateTotals(inv);
            Invoice created = ctx.getInvoiceService().createInvoice(inv);

            UIFactory.showInfoDialog("Sale Complete",
                "Invoice " + created.getInvoiceNumber() + " created!\nTotal: " +
                CurrencyFormatter.format(created.getTotalAmount()) + "\nChange: " +
                CurrencyFormatter.format(Math.max(0, tendered - created.getTotalAmount())));

            clearCart();
        } catch (Exception ex) {
            UIFactory.showErrorDialog("Sale Failed", ex.getMessage());
        }
    }

    // ==================== MEDICINE LIST ====================

    private void loadMedicines() {
        Thread t = new Thread(() -> {
            List<Medicine> meds = ctx.getMedicineService().getAllMedicines();
            Platform.runLater(() -> {
                allMedicines = meds;
                medicineListView.setItems(FXCollections.observableArrayList(meds));
            });
        });
        t.setDaemon(true);
        t.start();
    }

    private void filterMedicineList(String query) {
        if (allMedicines == null) return;
        if (query == null || query.isBlank()) {
            medicineListView.setItems(FXCollections.observableArrayList(allMedicines));
            return;
        }
        String q = query.toLowerCase();
        medicineListView.setItems(FXCollections.observableArrayList(
            allMedicines.stream()
                .filter(m -> m.getName().toLowerCase().contains(q) ||
                             (m.getGenericName() != null && m.getGenericName().toLowerCase().contains(q)) ||
                             m.getBatchNumber().toLowerCase().contains(q))
                .toList()
        ));
    }
}
