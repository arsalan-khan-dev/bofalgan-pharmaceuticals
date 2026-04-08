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
//  SupplierController
// =====================================================================
public class SupplierController {

    private final AppContext     ctx;
    private final MainController main;
    private TableView<Supplier>  table;
    private ObservableList<Supplier> items;
    private TextField            searchField;

    public SupplierController(AppContext ctx, MainController main) {
        this.ctx = ctx; this.main = main;
    }

    public Node buildView() {
        VBox view = new VBox(0);
        view.setStyle("-fx-background-color: #F8FAFC;");
        view.getChildren().addAll(buildToolbar(), buildContent());
        loadSuppliers();
        return view;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;");
        Label title = UIFactory.createH2("Suppliers");
        searchField = UIFactory.createTextField("Search suppliers...");
        searchField.setPrefWidth(250);
        searchField.textProperty().addListener((o, ov, nv) -> loadSuppliersFiltered(nv));
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = UIFactory.createButton("+ Add Supplier", UIFactory.ButtonType.PRIMARY);
        addBtn.setOnAction(e -> showSupplierForm(null));
        bar.getChildren().addAll(title, searchField, spacer, addBtn);
        return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(0);
        content.setPadding(new Insets(16, 24, 16, 24));
        VBox.setVgrow(content, Priority.ALWAYS);
        table = UIFactory.createStyledTable();
        items = FXCollections.observableArrayList();
        table.setItems(items);
        addCol("Name", 200, s -> s.getName());
        addCol("Contact", 130, s -> s.getContactPerson() != null ? s.getContactPerson() : "");
        addCol("Phone", 110, s -> s.getPhone() != null ? s.getPhone() : "");
        addCol("Email", 160, s -> s.getEmail() != null ? s.getEmail() : "");
        addCol("City", 100, s -> s.getCity() != null ? s.getCity() : "");
        addCol("Terms", 90, s -> s.getPaymentTerms() != null ? s.getPaymentTerms() : "");
        addCol("Status", 80, s -> s.isActive() ? "Active" : "Inactive");

        TableColumn<Supplier, Void> actCol = new TableColumn<>("Actions");
        actCol.setPrefWidth(140);
        actCol.setCellFactory(col -> new TableCell<Supplier, Void>() {
            final Button editBtn     = UIFactory.createButton("Edit", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.SMALL);
            final Button deactivateBtn = UIFactory.createButton("Deactivate", UIFactory.ButtonType.DANGER, UIFactory.ButtonSize.SMALL);
            final HBox pane = new HBox(6, editBtn, deactivateBtn);
            { pane.setAlignment(Pos.CENTER);
              editBtn.setOnAction(e -> showSupplierForm(getTableView().getItems().get(getIndex())));
              deactivateBtn.setOnAction(e -> handleDeactivate(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty ? null : pane); }
        });
        table.getColumns().add(actCol);
        VBox.setVgrow(table, Priority.ALWAYS);
        content.getChildren().add(table);
        return content;
    }

    private void addCol(String h, double w, java.util.function.Function<Supplier,String> fn) {
        TableColumn<Supplier, String> col = new TableColumn<>(h);
        col.setPrefWidth(w);
        col.setCellValueFactory(d -> new SimpleStringProperty(fn.apply(d.getValue())));
        table.getColumns().add(col);
    }

    private void loadSuppliers() { loadSuppliersFiltered(null); }
    private void loadSuppliersFiltered(String q) {
        Thread t = new Thread(() -> {
            List<Supplier> data = (q == null || q.isBlank()) ?
                ctx.getSupplierService().getAllSuppliers() :
                ctx.getSupplierService().searchSuppliers(q);
            Platform.runLater(() -> items.setAll(data));
        });
        t.setDaemon(true); t.start();
    }

    private void showSupplierForm(Supplier existing) {
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing == null ? "Add Supplier" : "Edit Supplier");
        VBox form = new VBox(12);
        form.setPadding(new Insets(24));
        form.setStyle("-fx-background-color: white;");
        form.getChildren().add(UIFactory.createH2(existing == null ? "Add Supplier" : "Edit Supplier"));

        TextField nameField    = UIFactory.createTextField("Company name");
        TextField contactField = UIFactory.createTextField("Contact person");
        TextField phoneField   = UIFactory.createTextField("Phone number");
        TextField emailField   = UIFactory.createTextField("Email address");
        TextArea  addressArea  = UIFactory.createTextArea("Full address");
        TextField cityField    = UIFactory.createTextField("City");
        TextField stateField   = UIFactory.createTextField("State");
        TextField gstinField   = UIFactory.createTextField("GSTIN/Tax ID (optional)");
        ComboBox<String> termsCombo = UIFactory.createComboBox();
        termsCombo.getItems().addAll("Net 30","Net 60","Net 90","COD","Advance","Other");

        if (existing != null) {
            nameField.setText(existing.getName());
            contactField.setText(existing.getContactPerson() != null ? existing.getContactPerson() : "");
            phoneField.setText(existing.getPhone() != null ? existing.getPhone() : "");
            emailField.setText(existing.getEmail() != null ? existing.getEmail() : "");
            addressArea.setText(existing.getAddress() != null ? existing.getAddress() : "");
            cityField.setText(existing.getCity() != null ? existing.getCity() : "");
            stateField.setText(existing.getState() != null ? existing.getState() : "");
            gstinField.setText(existing.getGstin() != null ? existing.getGstin() : "");
            termsCombo.setValue(existing.getPaymentTerms());
        }

        GridPane grid = new GridPane();
        grid.setHgap(12); grid.setVgap(8);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(UIFactory.createFormField("Company Name *", nameField), 0, 0);
        grid.add(UIFactory.createFormField("Contact Person", contactField), 1, 0);
        grid.add(UIFactory.createFormField("Phone", phoneField), 0, 1);
        grid.add(UIFactory.createFormField("Email", emailField), 1, 1);
        grid.add(UIFactory.createFormField("City", cityField), 0, 2);
        grid.add(UIFactory.createFormField("State", stateField), 1, 2);
        grid.add(UIFactory.createFormField("GSTIN", gstinField), 0, 3);
        grid.add(UIFactory.createFormField("Payment Terms", termsCombo), 1, 3);
        nameField.setMaxWidth(Double.MAX_VALUE); contactField.setMaxWidth(Double.MAX_VALUE);
        phoneField.setMaxWidth(Double.MAX_VALUE); emailField.setMaxWidth(Double.MAX_VALUE);
        cityField.setMaxWidth(Double.MAX_VALUE); stateField.setMaxWidth(Double.MAX_VALUE);
        gstinField.setMaxWidth(Double.MAX_VALUE); termsCombo.setMaxWidth(Double.MAX_VALUE);

        Label errLbl = new Label(""); errLbl.setTextFill(Color.web(AppConfig.COLOR_ERROR)); errLbl.setVisible(false);
        Button saveBtn = UIFactory.createButton("Save", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.LARGE);
        Button cancelBtn = UIFactory.createButton("Cancel", UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
        HBox btns = new HBox(10, cancelBtn, saveBtn); btns.setAlignment(Pos.CENTER_RIGHT);
        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> {
            try {
                Supplier s = existing != null ? existing : new Supplier();
                s.setName(nameField.getText().trim());
                s.setContactPerson(contactField.getText().trim());
                s.setPhone(phoneField.getText().trim());
                s.setEmail(emailField.getText().trim());
                s.setAddress(addressArea.getText().trim());
                s.setCity(cityField.getText().trim());
                s.setState(stateField.getText().trim());
                s.setGstin(gstinField.getText().trim());
                s.setPaymentTerms(termsCombo.getValue());
                if (existing == null) ctx.getSupplierService().addSupplier(s);
                else                  ctx.getSupplierService().updateSupplier(s);
                dialog.close(); loadSuppliers();
            } catch (Exception ex) { errLbl.setText("⚠ " + ex.getMessage()); errLbl.setVisible(true); }
        });
        form.getChildren().addAll(grid, UIFactory.createFormField("Address", addressArea), errLbl, btns);
        dialog.setScene(new javafx.scene.Scene(new ScrollPane(form) {{ setFitToWidth(true); setStyle("-fx-background: white;"); }}));
        dialog.setMinWidth(580); dialog.setMinHeight(480);
        dialog.showAndWait();
    }

    private void handleDeactivate(Supplier s) {
        if (!UIFactory.showConfirmDialog("Deactivate Supplier", "Deactivate '" + s.getName() + "'?")) return;
        try { ctx.getSupplierService().deactivateSupplier(s.getId()); loadSuppliers(); }
        catch (Exception ex) { UIFactory.showErrorDialog("Error", ex.getMessage()); }
    }
}
