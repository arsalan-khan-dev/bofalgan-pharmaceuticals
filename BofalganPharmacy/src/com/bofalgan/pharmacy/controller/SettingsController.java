package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.service.SessionManager;
import com.bofalgan.pharmacy.ui.UIFactory;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.io.IOException;
import java.nio.file.*;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

public class SettingsController {

    private final AppContext ctx; private final MainController main;
    // Settings fields
    private TextField pharmacyNameField, licenseField, addressField, phoneField;
    private TextField reorderLevelField, expiryWarningField, expiryCriticalField;
    private TextField currencyField, invoicePrefixField, invoiceFooterField, taxRateField;
    private TextField autoLogoutField, maxFailedField, lockDurationField;
    private Label backupStatusLabel;

    public SettingsController(AppContext ctx, MainController main) { this.ctx=ctx; this.main=main; }

    public Node buildView() {
        if (!SessionManager.getInstance().isAdmin()) {
            VBox denied = new VBox(20); denied.setAlignment(Pos.CENTER);
            denied.getChildren().add(new Label("⛔ Admin access required.")); return denied;
        }
        VBox view = new VBox(0); view.setStyle("-fx-background-color:#F8FAFC;");
        HBox toolbar = new HBox(10); toolbar.setAlignment(Pos.CENTER_LEFT);
        toolbar.setPadding(new Insets(14,24,14,24));
        toolbar.setStyle("-fx-background-color:white;-fx-border-color:"+AppConfig.COLOR_BORDER+";-fx-border-width:0 0 1 0;");
        toolbar.getChildren().add(UIFactory.createH2("Settings"));
        view.getChildren().add(toolbar);

        ScrollPane scroll = new ScrollPane(); scroll.setFitToWidth(true);
        scroll.setStyle("-fx-background-color:transparent;-fx-background:transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(20); content.setPadding(new Insets(24));
        content.getChildren().addAll(
            buildSection("Pharmacy Profile",    buildProfileSection()),
            buildSection("Inventory Defaults",  buildInventorySection()),
            buildSection("Invoice & Billing",   buildBillingSection()),
            buildSection("Session & Security",  buildSecuritySection()),
            buildSection("Backup & Restore",    buildBackupSection())
        );

        // Global save button
        Button saveAllBtn = UIFactory.createButton("Save All Settings", UIFactory.ButtonType.SUCCESS, UIFactory.ButtonSize.LARGE);
        saveAllBtn.setOnAction(e -> saveAllSettings());
        HBox saveRow = new HBox(saveAllBtn); saveRow.setAlignment(Pos.CENTER_RIGHT);
        content.getChildren().add(saveRow);

        scroll.setContent(content); view.getChildren().add(scroll);
        loadSettings(); return view;
    }

    private VBox buildSection(String title, GridPane grid) {
        VBox section = new VBox(12);
        section.setStyle("-fx-background-color:white;-fx-border-color:"+AppConfig.COLOR_BORDER+
            ";-fx-border-radius:8;-fx-background-radius:8;-fx-padding:16;");
        Label titleLbl = UIFactory.createH3(title);
        section.getChildren().addAll(titleLbl, UIFactory.createSeparator(), grid);
        return section;
    }

    private GridPane buildProfileSection() {
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        pharmacyNameField = UIFactory.createTextField("Pharmacy name"); pharmacyNameField.setMaxWidth(Double.MAX_VALUE);
        licenseField      = UIFactory.createTextField("License #");     licenseField.setMaxWidth(Double.MAX_VALUE);
        addressField      = UIFactory.createTextField("Address");       addressField.setMaxWidth(Double.MAX_VALUE);
        phoneField        = UIFactory.createTextField("Phone");         phoneField.setMaxWidth(Double.MAX_VALUE);
        grid.add(UIFactory.createFormField("Pharmacy Name", pharmacyNameField), 0, 0);
        grid.add(UIFactory.createFormField("License #", licenseField), 1, 0);
        grid.add(UIFactory.createFormField("Address", addressField), 0, 1);
        grid.add(UIFactory.createFormField("Phone", phoneField), 1, 1);
        return grid;
    }

    private GridPane buildInventorySection() {
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(33);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(33);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(33);
        grid.getColumnConstraints().addAll(c1, c2, c3);
        reorderLevelField   = UIFactory.createTextField("10"); reorderLevelField.setMaxWidth(Double.MAX_VALUE);
        expiryWarningField  = UIFactory.createTextField("30"); expiryWarningField.setMaxWidth(Double.MAX_VALUE);
        expiryCriticalField = UIFactory.createTextField("7");  expiryCriticalField.setMaxWidth(Double.MAX_VALUE);
        grid.add(UIFactory.createFormField("Default Reorder Level", reorderLevelField), 0, 0);
        grid.add(UIFactory.createFormField("Expiry Warning (days)", expiryWarningField), 1, 0);
        grid.add(UIFactory.createFormField("Expiry Critical (days)", expiryCriticalField), 2, 0);
        return grid;
    }

    private GridPane buildBillingSection() {
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        currencyField     = UIFactory.createTextField("$"); currencyField.setMaxWidth(Double.MAX_VALUE);
        invoicePrefixField= UIFactory.createTextField("INV"); invoicePrefixField.setMaxWidth(Double.MAX_VALUE);
        invoiceFooterField= UIFactory.createTextField("Thank you!"); invoiceFooterField.setMaxWidth(Double.MAX_VALUE);
        taxRateField      = UIFactory.createTextField("0"); taxRateField.setMaxWidth(Double.MAX_VALUE);
        grid.add(UIFactory.createFormField("Currency Symbol", currencyField), 0, 0);
        grid.add(UIFactory.createFormField("Invoice Prefix", invoicePrefixField), 1, 0);
        grid.add(UIFactory.createFormField("Default Tax Rate (%)", taxRateField), 0, 1);
        grid.add(UIFactory.createFormField("Invoice Footer Text", invoiceFooterField), 1, 1);
        return grid;
    }

    private GridPane buildSecuritySection() {
        GridPane grid = new GridPane(); grid.setHgap(16); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(33);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(33);
        ColumnConstraints c3 = new ColumnConstraints(); c3.setPercentWidth(33);
        grid.getColumnConstraints().addAll(c1, c2, c3);
        autoLogoutField  = UIFactory.createTextField("30"); autoLogoutField.setMaxWidth(Double.MAX_VALUE);
        maxFailedField   = UIFactory.createTextField("3");  maxFailedField.setMaxWidth(Double.MAX_VALUE);
        lockDurationField= UIFactory.createTextField("15"); lockDurationField.setMaxWidth(Double.MAX_VALUE);
        grid.add(UIFactory.createFormField("Auto-Logout (mins, 0=off)", autoLogoutField), 0, 0);
        grid.add(UIFactory.createFormField("Max Failed Logins", maxFailedField), 1, 0);
        grid.add(UIFactory.createFormField("Lock Duration (mins)", lockDurationField), 2, 0);
        return grid;
    }

    private VBox buildBackupSection() {
        VBox section = new VBox(10);
        backupStatusLabel = new Label("No backup performed yet.");
        backupStatusLabel.setFont(UIConstants.small()); backupStatusLabel.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));

        Button backupBtn  = UIFactory.createButton("Backup Now", UIFactory.ButtonType.SUCCESS);
        Button restoreBtn = UIFactory.createButton("Restore from File", UIFactory.ButtonType.WARNING);

        backupBtn.setOnAction(e  -> performBackup());
        restoreBtn.setOnAction(e -> UIFactory.showInfoDialog("Restore", "To restore, copy backup files from your backup folder to the data/ directory and restart the application."));

        HBox btnRow = new HBox(10, backupBtn, restoreBtn); btnRow.setAlignment(Pos.CENTER_LEFT);
        GridPane grid = new GridPane(); // Return as VBox wrapper
        section.getChildren().addAll(backupStatusLabel, btnRow);
        return section;
    }

    private void loadSettings() {
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT `key`, value FROM settings")) {
            Map<String,String> s = new LinkedHashMap<>();
            while (rs.next()) s.put(rs.getString("key"), rs.getString("value"));
            if (pharmacyNameField != null) pharmacyNameField.setText(s.getOrDefault("pharmacy_name",""));
            if (licenseField != null)      licenseField.setText(s.getOrDefault("pharmacy_license",""));
            if (addressField != null)      addressField.setText(s.getOrDefault("pharmacy_address",""));
            if (phoneField != null)        phoneField.setText(s.getOrDefault("pharmacy_phone",""));
            if (reorderLevelField != null) reorderLevelField.setText(s.getOrDefault("reorder_level","10"));
            if (expiryWarningField != null) expiryWarningField.setText(s.getOrDefault("expiry_warning_days","30"));
            if (expiryCriticalField != null) expiryCriticalField.setText(s.getOrDefault("expiry_critical_days","7"));
            if (currencyField != null)     currencyField.setText(s.getOrDefault("currency_symbol","$"));
            if (invoicePrefixField != null) invoicePrefixField.setText(s.getOrDefault("invoice_prefix","INV"));
            if (invoiceFooterField != null) invoiceFooterField.setText(s.getOrDefault("invoice_footer","Thank you!"));
            if (taxRateField != null)      taxRateField.setText(s.getOrDefault("default_tax_rate","0"));
            if (autoLogoutField != null)   autoLogoutField.setText(s.getOrDefault("auto_logout_minutes","30"));
            if (maxFailedField != null)    maxFailedField.setText(s.getOrDefault("max_failed_logins","3"));
            if (lockDurationField != null) lockDurationField.setText(s.getOrDefault("lock_duration_mins","15"));
        } catch (Exception ex) { System.err.println("[Settings] Load: " + ex.getMessage()); }
    }

    private void saveAllSettings() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            Map<String,String> settings = new LinkedHashMap<>();
            settings.put("pharmacy_name", pharmacyNameField.getText());
            settings.put("pharmacy_license", licenseField.getText());
            settings.put("pharmacy_address", addressField.getText());
            settings.put("pharmacy_phone", phoneField.getText());
            settings.put("reorder_level", reorderLevelField.getText());
            settings.put("expiry_warning_days", expiryWarningField.getText());
            settings.put("expiry_critical_days", expiryCriticalField.getText());
            settings.put("currency_symbol", currencyField.getText());
            settings.put("invoice_prefix", invoicePrefixField.getText());
            settings.put("invoice_footer", invoiceFooterField.getText());
            settings.put("default_tax_rate", taxRateField.getText());
            settings.put("auto_logout_minutes", autoLogoutField.getText());
            settings.put("max_failed_logins", maxFailedField.getText());
            settings.put("lock_duration_mins", lockDurationField.getText());
            String sql = "INSERT INTO settings (`key`, value) VALUES (?,?) ON DUPLICATE KEY UPDATE value=?";
            try (PreparedStatement ps = conn.prepareStatement(sql)) {
                for (Map.Entry<String,String> e : settings.entrySet()) {
                    ps.setString(1, e.getKey()); ps.setString(2, e.getValue()); ps.setString(3, e.getValue());
                    ps.addBatch();
                }
                ps.executeBatch();
            }
            UIFactory.showInfoDialog("Settings Saved", "All settings have been saved successfully.");
        } catch (Exception ex) { UIFactory.showErrorDialog("Save Error", ex.getMessage()); }
    }

    private void performBackup() {
        try {
            String backupDir = AppConfig.BACKUP_DIR;
            Files.createDirectories(Paths.get(backupDir));
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String destDir = backupDir + "/backup_" + timestamp;
            Files.createDirectories(Paths.get(destDir));
            // Copy all data files
            File dataDir = new File(AppConfig.DATA_DIR);
            if (dataDir.exists()) {
                for (File f : dataDir.listFiles()) {
                    Files.copy(f.toPath(), Paths.get(destDir, f.getName()), StandardCopyOption.REPLACE_EXISTING);
                }
            }
            backupStatusLabel.setText("Last backup: " + timestamp + " → " + destDir);
            backupStatusLabel.setTextFill(Color.web(AppConfig.COLOR_SUCCESS));
            UIFactory.showInfoDialog("Backup Complete","Backup saved to:\n" + destDir);
        } catch (IOException ex) {
            UIFactory.showErrorDialog("Backup Failed", ex.getMessage());
        }
    }
}
