package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.model.Medicine;
import com.bofalgan.pharmacy.model.Supplier;
import com.bofalgan.pharmacy.service.SessionManager;
import com.bofalgan.pharmacy.ui.UIFactory;
import com.bofalgan.pharmacy.util.*;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.*;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.*;

import java.time.LocalDate;
import java.util.List;

public class MedicineController {

    private final AppContext     ctx;
    private final MainController main;
    private TableView<Medicine>  table;
    private ObservableList<Medicine> items;
    private TextField            searchField;
    private Label                statusLabel;

    public MedicineController(AppContext ctx, MainController main) {
        this.ctx  = ctx;
        this.main = main;
    }

    public Node buildView() {
        VBox view = new VBox(0);
        view.setStyle("-fx-background-color: #F8FAFC;");

        view.getChildren().addAll(buildToolbar(), buildContent());
        loadMedicines(null);
        return view;
    }

    // ==================== TOOLBAR ====================

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;");

        Label title = UIFactory.createH2("Medicine Inventory");

        searchField = UIFactory.createTextField("Search medicines...");
        searchField.setPrefWidth(280);
        searchField.textProperty().addListener((obs, old, val) -> {
            javafx.animation.PauseTransition pt = new javafx.animation.PauseTransition(javafx.util.Duration.millis(300));
            pt.setOnFinished(e -> loadMedicines(val));
            pt.playFromStart();
        });

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button addBtn    = UIFactory.createButton("+ Add Medicine", UIFactory.ButtonType.PRIMARY);
        Button exportBtn = UIFactory.createButton("Export Excel",   UIFactory.ButtonType.SECONDARY);
        Button pdfBtn    = UIFactory.createButton("Export PDF",     UIFactory.ButtonType.SECONDARY);

        addBtn.setOnAction(e    -> showMedicineForm(null));
        exportBtn.setOnAction(e -> handleExportExcel());
        pdfBtn.setOnAction(e    -> handleExportPDF());

        statusLabel = new Label("");
        statusLabel.setFont(UIConstants.small());
        statusLabel.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));

        bar.getChildren().addAll(title, searchField, spacer, statusLabel, exportBtn, pdfBtn, addBtn);
        return bar;
    }

    // ==================== CONTENT ====================

    private VBox buildContent() {
        VBox content = new VBox(0);
        VBox.setVgrow(content, Priority.ALWAYS);
        content.setPadding(new Insets(16, 24, 16, 24));

        table = UIFactory.createStyledTable();
        items = FXCollections.observableArrayList();
        table.setItems(items);

        addColumn("#",           50,  m -> String.valueOf(items.indexOf(m) + 1));
        addColumn("Name",        200, Medicine::getName);
        addColumn("Generic",     140, m -> m.getGenericName() != null ? m.getGenericName() : "");
        addColumn("Category",    110, m -> m.getCategory() != null ? m.getCategory() : "");
        addColumn("Unit",         70, m -> m.getUnit() != null ? m.getUnit() : "");
        addColumn("Batch",        90, Medicine::getBatchNumber);
        addColumn("Qty",          60, m -> String.valueOf(m.getQuantity()));
        addColumn("Reorder",      60, m -> String.valueOf(m.getReorderLevel()));
        if (SessionManager.getInstance().isAdmin()) {
            addColumn("Purchase $", 90, m -> CurrencyFormatter.formatNoSymbol(m.getPurchasePrice()));
        }
        addColumn("Sell $",      90, m -> CurrencyFormatter.formatNoSymbol(m.getSellingPrice()));
        addColumn("Expiry",     100, m -> m.getExpiryDate() != null ? DateUtils.format(m.getExpiryDate()) : "");
        addColumn("Status",      110, Medicine::getExpiryStatus);

        // Color coding by expiry
        table.setRowFactory(tv -> new TableRow<Medicine>() {
            @Override
            protected void updateItem(Medicine m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) { setStyle(""); return; }
                String color = m.getExpiryColorCode();
                if ("RED".equals(color))    setStyle("-fx-background-color: #FFEBEE;");
                else if ("YELLOW".equals(color)) setStyle("-fx-background-color: #FFFDE7;");
                else setStyle(getIndex() % 2 == 0 ? "" : "-fx-background-color: " + AppConfig.COLOR_ROW_ALT + ";");
            }
        });

        // Actions column
        TableColumn<Medicine, Void> actionsCol = new TableColumn<>("Actions");
        actionsCol.setPrefWidth(160);
        actionsCol.setCellFactory(col -> new TableCell<Medicine, Void>() {
            final Button editBtn   = UIFactory.createButton("Edit",   UIFactory.ButtonType.PRIMARY,   UIFactory.ButtonSize.SMALL);
            final Button deleteBtn = UIFactory.createButton("Delete", UIFactory.ButtonType.DANGER,    UIFactory.ButtonSize.SMALL);
            final HBox pane = new HBox(6, editBtn, deleteBtn);
            {
                pane.setAlignment(Pos.CENTER);
                editBtn.setOnAction(e -> showMedicineForm(getTableView().getItems().get(getIndex())));
                deleteBtn.setOnAction(e -> handleDelete(getTableView().getItems().get(getIndex())));
                deleteBtn.setVisible(SessionManager.getInstance().isAdmin());
            }
            @Override protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        table.getColumns().add(actionsCol);

        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
        return content;
    }

    private void addColumn(String header, double width, java.util.function.Function<Medicine, String> extractor) {
        TableColumn<Medicine, String> col = new TableColumn<>(header);
        col.setPrefWidth(width);
        col.setCellValueFactory(data -> new SimpleStringProperty(extractor.apply(data.getValue())));
        table.getColumns().add(col);
    }

    // ==================== DATA LOADING ====================

    private void loadMedicines(String query) {
        Thread t = new Thread(() -> {
            try {
                List<Medicine> data = (query == null || query.isBlank()) ?
                    ctx.getMedicineService().getAllMedicines() :
                    ctx.getMedicineService().searchMedicines(query);
                Platform.runLater(() -> {
                    items.setAll(data);
                    statusLabel.setText(data.size() + " medicines");
                });
            } catch (Exception ex) {
                Platform.runLater(() -> UIFactory.showErrorDialog("Error", ex.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ==================== ADD / EDIT FORM ====================

    private void showMedicineForm(Medicine existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Medicine" : "Edit Medicine: " + existing.getName());
        dialog.setMinWidth(600);
        dialog.setMinHeight(640);

        VBox form = new VBox(14);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");

        Label heading = UIFactory.createH2(existing == null ? "Add New Medicine" : "Edit Medicine");
        form.getChildren().add(heading);
        form.getChildren().add(UIFactory.createSeparator());

        // --- Fields ---
        TextField nameField    = UIFactory.createTextField("e.g. Paracetamol 500mg");
        TextField genericField = UIFactory.createTextField("e.g. Acetaminophen");
        TextField strengthField= UIFactory.createTextField("e.g. 500mg");
        TextField batchField   = UIFactory.createTextField("e.g. BATCH2024001");
        TextField qtyField     = UIFactory.createTextField("0");
        TextField reorderField = UIFactory.createTextField("10");
        TextField purchPriceF  = UIFactory.createTextField("0.00");
        TextField sellPriceF   = UIFactory.createTextField("0.00");
        TextField storageField = UIFactory.createTextField("e.g. Shelf A1");
        TextField barcodeField = UIFactory.createTextField("(optional)");
        DatePicker expiryPicker= new DatePicker();
        expiryPicker.setMaxWidth(Double.MAX_VALUE);
        expiryPicker.setPrefHeight(UIConstants.INPUT_HEIGHT);

        ComboBox<String> unitCombo = UIFactory.createComboBox();
        unitCombo.getItems().addAll("Tablet","Capsule","Syrup","Injection","Strip","Cream","Drops","Sachet","Vial","Other");

        ComboBox<String> categoryCombo = UIFactory.createComboBox();
        ctx.getMedicineService().getAllCategories().forEach(categoryCombo.getItems()::add);

        ComboBox<String> supplierCombo = UIFactory.createComboBox();
        List<Supplier> suppliers = ctx.getSupplierService().getActiveSuppliers();
        supplierCombo.getItems().add("-- None --");
        suppliers.forEach(s -> supplierCombo.getItems().add(s.getId() + " | " + s.getName()));

        CheckBox rxCheck = new CheckBox("Prescription Only");

        // Pre-fill if editing
        if (existing != null) {
            nameField.setText(existing.getName());
            genericField.setText(existing.getGenericName() != null ? existing.getGenericName() : "");
            strengthField.setText(existing.getStrength() != null ? existing.getStrength() : "");
            batchField.setText(existing.getBatchNumber());
            batchField.setDisable(true);
            qtyField.setText(String.valueOf(existing.getQuantity()));
            reorderField.setText(String.valueOf(existing.getReorderLevel()));
            purchPriceF.setText(CurrencyFormatter.formatNoSymbol(existing.getPurchasePrice()));
            sellPriceF.setText(CurrencyFormatter.formatNoSymbol(existing.getSellingPrice()));
            storageField.setText(existing.getStorageLocation() != null ? existing.getStorageLocation() : "");
            barcodeField.setText(existing.getBarcode() != null ? existing.getBarcode() : "");
            expiryPicker.setValue(existing.getExpiryDate());
            unitCombo.setValue(existing.getUnit());
            categoryCombo.setValue(existing.getCategory());
            rxCheck.setSelected(existing.isPrescriptionOnly());
            if (existing.getSupplierId() > 0) {
                suppliers.stream()
                    .filter(s -> s.getId() == existing.getSupplierId())
                    .findFirst()
                    .ifPresent(s -> supplierCombo.setValue(s.getId() + " | " + s.getName()));
            }
        }

        // Margin label
        Label marginLabel = new Label("Margin: 0%");
        marginLabel.setFont(UIConstants.small());
        marginLabel.setTextFill(Color.web(AppConfig.COLOR_SUCCESS));
        Runnable updateMargin = () -> {
            try {
                double pp = Double.parseDouble(purchPriceF.getText().replace(",",""));
                double sp = Double.parseDouble(sellPriceF.getText().replace(",",""));
                if (pp > 0) {
                    double margin = ((sp - pp) / pp) * 100;
                    marginLabel.setText(String.format("Margin: %.1f%%", margin));
                    marginLabel.setTextFill(Color.web(margin >= 0 ? AppConfig.COLOR_SUCCESS : AppConfig.COLOR_ERROR));
                }
            } catch (Exception ignored) {}
        };
        purchPriceF.textProperty().addListener((o,ov,nv) -> updateMargin.run());
        sellPriceF.textProperty().addListener((o,ov,nv)  -> updateMargin.run());

        // Layout grid
        GridPane grid = new GridPane();
        grid.setHgap(16); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);

        addGridRow(grid, 0, "Medicine Name *", nameField, "Generic Name", genericField);
        addGridRow(grid, 1, "Strength",        strengthField, "Batch Number *", batchField);
        addGridRow(grid, 2, "Unit *",          unitCombo,     "Category",        categoryCombo);
        addGridRow(grid, 3, "Quantity *",      qtyField,      "Reorder Level",   reorderField);
        addGridRow(grid, 4, "Purchase Price *",purchPriceF,   "Selling Price *",  sellPriceF);
        addGridRow(grid, 5, "Expiry Date *",   expiryPicker,  "Supplier",        supplierCombo);
        addGridRow(grid, 6, "Storage Location",storageField,  "Barcode",         barcodeField);

        // Error label
        Label errorLbl = new Label("");
        errorLbl.setFont(UIConstants.body());
        errorLbl.setTextFill(Color.web(AppConfig.COLOR_ERROR));
        errorLbl.setWrapText(true);
        errorLbl.setVisible(false);

        // Buttons
        Button saveBtn   = UIFactory.createButton(existing == null ? "Add Medicine" : "Save Changes",
            UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.LARGE);
        Button cancelBtn = UIFactory.createButton("Cancel", UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
        HBox btns = new HBox(10, cancelBtn, saveBtn);
        btns.setAlignment(Pos.CENTER_RIGHT);
        btns.setPadding(new Insets(8, 0, 0, 0));
        cancelBtn.setOnAction(e -> dialog.close());

        saveBtn.setOnAction(e -> {
            try {
                Medicine m = existing != null ? new Medicine() : new Medicine();
                if (existing != null) m.setId(existing.getId());

                m.setName(nameField.getText().trim());
                m.setGenericName(genericField.getText().trim());
                m.setStrength(strengthField.getText().trim());
                m.setBatchNumber(batchField.getText().trim());
                m.setQuantity(Integer.parseInt(qtyField.getText().trim()));
                m.setReorderLevel(Integer.parseInt(reorderField.getText().trim()));
                m.setPurchasePrice(Double.parseDouble(purchPriceF.getText().replace(",","")));
                m.setSellingPrice(Double.parseDouble(sellPriceF.getText().replace(",","")));
                m.setUnit(unitCombo.getValue());
                m.setCategory(categoryCombo.getValue());
                m.setExpiryDate(expiryPicker.getValue());
                m.setStorageLocation(storageField.getText().trim());
                m.setBarcode(barcodeField.getText().trim().isEmpty() ? null : barcodeField.getText().trim());
                m.setPrescriptionOnly(rxCheck.isSelected());

                String supVal = supplierCombo.getValue();
                if (supVal != null && !supVal.startsWith("--")) {
                    m.setSupplierId(Integer.parseInt(supVal.split("\\|")[0].trim()));
                }

                if (existing == null) ctx.getMedicineService().addMedicine(m);
                else                  ctx.getMedicineService().updateMedicine(m);

                dialog.close();
                loadMedicines(null);
            } catch (ValidationException ex) {
                errorLbl.setText("⚠ " + ex.getMessage());
                errorLbl.setVisible(true);
            } catch (NumberFormatException ex) {
                errorLbl.setText("⚠ Please enter valid numeric values for Qty, Prices.");
                errorLbl.setVisible(true);
            } catch (Exception ex) {
                errorLbl.setText("⚠ " + ex.getMessage());
                errorLbl.setVisible(true);
            }
        });

        form.getChildren().addAll(grid, rxCheck, marginLabel, errorLbl, btns);

        ScrollPane sp = new ScrollPane(form);
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: white; -fx-background: white;");

        dialog.setScene(new javafx.scene.Scene(sp));
        dialog.showAndWait();
    }

    private void addGridRow(GridPane grid, int row, String lbl1, Node ctrl1, String lbl2, Node ctrl2) {
        VBox left  = UIFactory.createFormField(lbl1, ctrl1);
        VBox right = UIFactory.createFormField(lbl2, ctrl2);
        ctrl1.setStyle(getClass().getSimpleName()); // preserve width
        ((Region) ctrl1).setMaxWidth(Double.MAX_VALUE);
        ((Region) ctrl2).setMaxWidth(Double.MAX_VALUE);
        grid.add(left, 0, row);
        grid.add(right, 1, row);
    }

    // ==================== DELETE ====================

    private void handleDelete(Medicine m) {
        if (!SessionManager.getInstance().isAdmin()) {
            UIFactory.showErrorDialog("Permission Denied", "Only admins can delete medicines.");
            return;
        }
        boolean confirm = UIFactory.showConfirmDialog("Delete Medicine",
            "Delete '" + m.getName() + "'? This action will hide it from inventory.");
        if (!confirm) return;
        try {
            ctx.getMedicineService().deleteMedicine(m.getId());
            loadMedicines(null);
        } catch (Exception ex) {
            UIFactory.showErrorDialog("Error", ex.getMessage());
        }
    }

    // ==================== EXPORT ====================

    private void handleExportExcel() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save Excel File");
        fc.setInitialFileName("medicines_" + LocalDate.now() + ".xlsx");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("Excel Files","*.xlsx"));
        java.io.File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            ExcelExporter.exportMedicines(items, f.getAbsolutePath());
            UIFactory.showInfoDialog("Export Complete", "Exported " + items.size() + " medicines to:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            UIFactory.showErrorDialog("Export Failed", ex.getMessage());
        }
    }

    private void handleExportPDF() {
        javafx.stage.FileChooser fc = new javafx.stage.FileChooser();
        fc.setTitle("Save PDF File");
        fc.setInitialFileName("medicines_" + LocalDate.now() + ".pdf");
        fc.getExtensionFilters().add(new javafx.stage.FileChooser.ExtensionFilter("PDF Files","*.pdf"));
        java.io.File f = fc.showSaveDialog(null);
        if (f == null) return;
        try {
            PDFExporter.generateInventoryReportPDF(items, f.getAbsolutePath());
            UIFactory.showInfoDialog("Export Complete", "PDF saved to:\n" + f.getAbsolutePath());
        } catch (Exception ex) {
            UIFactory.showErrorDialog("Export Failed", ex.getMessage());
        }
    }
}
