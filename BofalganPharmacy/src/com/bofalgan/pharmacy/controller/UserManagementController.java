package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.model.User;
import com.bofalgan.pharmacy.service.AuthService;
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.List;

// =====================================================================
//  UserManagementController
// =====================================================================
public class UserManagementController {

    private final AppContext ctx; private final MainController main;
    private TableView<User> table; private ObservableList<User> items;

    public UserManagementController(AppContext ctx, MainController main) { this.ctx=ctx; this.main=main; }

    public Node buildView() {
        if (!SessionManager.getInstance().isAdmin()) {
            VBox denied = new VBox(20); denied.setAlignment(Pos.CENTER);
            denied.getChildren().add(new Label("⛔ Admin access required.")); return denied;
        }
        VBox view = new VBox(0); view.setStyle("-fx-background-color:#F8FAFC;");
        view.getChildren().addAll(buildToolbar(), buildContent());
        loadUsers(); return view;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10); bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14,24,14,24));
        bar.setStyle("-fx-background-color:white;-fx-border-color:"+AppConfig.COLOR_BORDER+";-fx-border-width:0 0 1 0;");
        Label title = UIFactory.createH2("User Management");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button addBtn = UIFactory.createButton("+ Add User", UIFactory.ButtonType.PRIMARY);
        addBtn.setOnAction(e -> showUserForm(null));
        bar.getChildren().addAll(title, spacer, addBtn); return bar;
    }

    private VBox buildContent() {
        VBox content = new VBox(0); content.setPadding(new Insets(16,24,16,24)); VBox.setVgrow(content, Priority.ALWAYS);
        table = UIFactory.createStyledTable(); items = FXCollections.observableArrayList(); table.setItems(items);
        TableColumn<User,String> usernameCol = new TableColumn<>("Username"); usernameCol.setPrefWidth(120);
        usernameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getUsername()));
        TableColumn<User,String> nameCol = new TableColumn<>("Full Name"); nameCol.setPrefWidth(160);
        nameCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getFullName()));
        TableColumn<User,String> roleCol = new TableColumn<>("Role"); roleCol.setPrefWidth(80);
        roleCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getRole()));
        TableColumn<User,String> emailCol = new TableColumn<>("Email"); emailCol.setPrefWidth(160);
        emailCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().getEmail()!=null?d.getValue().getEmail():""));
        TableColumn<User,String> lastLoginCol = new TableColumn<>("Last Login"); lastLoginCol.setPrefWidth(140);
        lastLoginCol.setCellValueFactory(d -> new SimpleStringProperty(
            d.getValue().getLastLogin()!=null?DateUtils.format(d.getValue().getLastLogin()):"Never"));
        TableColumn<User,String> statusCol = new TableColumn<>("Status"); statusCol.setPrefWidth(80);
        statusCol.setCellValueFactory(d -> new SimpleStringProperty(d.getValue().isActive()?"Active":"Inactive"));

        TableColumn<User,Void> actCol = new TableColumn<>("Actions"); actCol.setPrefWidth(200);
        actCol.setCellFactory(col -> new TableCell<User,Void>() {
            final Button editBtn  = UIFactory.createButton("Edit", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.SMALL);
            final Button deactBtn = UIFactory.createButton("Deactivate", UIFactory.ButtonType.DANGER, UIFactory.ButtonSize.SMALL);
            final Button resetBtn = UIFactory.createButton("Reset Pwd", UIFactory.ButtonType.WARNING, UIFactory.ButtonSize.SMALL);
            final HBox pane = new HBox(4, editBtn, deactBtn, resetBtn);
            { pane.setAlignment(Pos.CENTER);
              editBtn.setOnAction(e  -> showUserForm(getTableView().getItems().get(getIndex())));
              deactBtn.setOnAction(e -> handleDeactivate(getTableView().getItems().get(getIndex())));
              resetBtn.setOnAction(e -> handleResetPassword(getTableView().getItems().get(getIndex())));
            }
            @Override protected void updateItem(Void v, boolean empty) { super.updateItem(v, empty); setGraphic(empty?null:pane); }
        });
        table.getColumns().addAll(usernameCol, nameCol, roleCol, emailCol, lastLoginCol, statusCol, actCol);
        VBox.setVgrow(table, Priority.ALWAYS); content.getChildren().add(table); return content;
    }

    private void loadUsers() {
        Thread t = new Thread(() -> {
            List<User> data = ctx.getUserService().getAllUsers();
            Platform.runLater(() -> items.setAll(data));
        }); t.setDaemon(true); t.start();
    }

    private void showUserForm(User existing) {
        Stage dialog = new Stage(); dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle(existing==null?"Add User":"Edit User");
        VBox form = new VBox(14); form.setPadding(new Insets(24)); form.setStyle("-fx-background-color:white;");
        form.getChildren().add(UIFactory.createH2(existing==null?"Add User":"Edit: "+existing.getUsername()));
        TextField usernameField = UIFactory.createTextField("Username");
        TextField fullNameField = UIFactory.createTextField("Full name");
        TextField emailField    = UIFactory.createTextField("Email");
        TextField phoneField    = UIFactory.createTextField("Phone");
        ComboBox<String> roleCombo = UIFactory.createComboBox();
        roleCombo.getItems().addAll("STAFF","ADMIN"); roleCombo.setValue("STAFF");
        PasswordField pwField = UIFactory.createPasswordField("Password (required for new users)");
        if (existing != null) {
            usernameField.setText(existing.getUsername()); usernameField.setDisable(true);
            fullNameField.setText(existing.getFullName());
            emailField.setText(existing.getEmail()!=null?existing.getEmail():"");
            phoneField.setText(existing.getPhone()!=null?existing.getPhone():"");
            roleCombo.setValue(existing.getRole());
        }
        Label errLbl = new Label(""); errLbl.setTextFill(Color.web(AppConfig.COLOR_ERROR)); errLbl.setVisible(false);
        Button saveBtn = UIFactory.createButton("Save", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.LARGE);
        Button cancelBtn = UIFactory.createButton("Cancel", UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
        HBox btns = new HBox(10, cancelBtn, saveBtn); btns.setAlignment(Pos.CENTER_RIGHT);
        cancelBtn.setOnAction(e -> dialog.close());
        saveBtn.setOnAction(e -> {
            try {
                User u = existing!=null ? existing : new User();
                u.setUsername(usernameField.getText().trim());
                u.setFullName(fullNameField.getText().trim());
                u.setEmail(emailField.getText().trim());
                u.setPhone(phoneField.getText().trim());
                u.setRole(roleCombo.getValue());
                if (existing == null) {
                    if (pwField.getText().isBlank()) throw new ValidationException("Password is required for new user.");
                    AuthService.validatePasswordStrength(pwField.getText());
                    u.setPasswordHash(pwField.getText()); // hashed in service
                    ctx.getUserService().addUser(u);
                } else {
                    ctx.getUserService().updateUser(u);
                }
                dialog.close(); loadUsers();
            } catch (Exception ex) { errLbl.setText("⚠ "+ex.getMessage()); errLbl.setVisible(true); }
        });
        GridPane grid = new GridPane(); grid.setHgap(12); grid.setVgap(10);
        ColumnConstraints c1 = new ColumnConstraints(); c1.setPercentWidth(50);
        ColumnConstraints c2 = new ColumnConstraints(); c2.setPercentWidth(50);
        grid.getColumnConstraints().addAll(c1, c2);
        grid.add(UIFactory.createFormField("Username *", usernameField), 0, 0);
        grid.add(UIFactory.createFormField("Full Name *", fullNameField), 1, 0);
        grid.add(UIFactory.createFormField("Email", emailField), 0, 1);
        grid.add(UIFactory.createFormField("Phone", phoneField), 1, 1);
        grid.add(UIFactory.createFormField("Role", roleCombo), 0, 2);
        if (existing==null) grid.add(UIFactory.createFormField("Password *", pwField), 1, 2);
        usernameField.setMaxWidth(Double.MAX_VALUE); fullNameField.setMaxWidth(Double.MAX_VALUE);
        emailField.setMaxWidth(Double.MAX_VALUE); phoneField.setMaxWidth(Double.MAX_VALUE);
        roleCombo.setMaxWidth(Double.MAX_VALUE); pwField.setMaxWidth(Double.MAX_VALUE);
        form.getChildren().addAll(grid, errLbl, btns);
        dialog.setScene(new javafx.scene.Scene(form, 560, 360)); dialog.showAndWait();
    }

    private void handleDeactivate(User u) {
        if (!UIFactory.showConfirmDialog("Deactivate","Deactivate user '"+u.getUsername()+"'?")) return;
        try { ctx.getUserService().deactivateUser(u.getId()); loadUsers(); }
        catch (Exception ex) { UIFactory.showErrorDialog("Error", ex.getMessage()); }
    }

    private void handleResetPassword(User u) {
        if (!UIFactory.showConfirmDialog("Reset Password","Reset password for '"+u.getUsername()+"'?")) return;
        try {
            String temp = ctx.getAuthService().adminResetPassword(u.getId());
            UIFactory.showInfoDialog("Password Reset","New temporary password:\n\n" + temp +
                "\n\nPlease give this to the user and ask them to change it.");
        } catch (Exception ex) { UIFactory.showErrorDialog("Error", ex.getMessage()); }
    }
}
