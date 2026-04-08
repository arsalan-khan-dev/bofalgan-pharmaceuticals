package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.service.AuthService;
import com.bofalgan.pharmacy.ui.UIFactory;
import com.bofalgan.pharmacy.util.ValidationException;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

public class LoginController {

    private final Stage      stage;
    private TextField        usernameField;
    private PasswordField    passwordField;
    private CheckBox         rememberMe;
    private Label            errorLabel;
    private Button           loginButton;

    public LoginController(Stage stage) {
        this.stage = stage;
    }

    public Scene buildScene() {
        // Root layout: two columns
        HBox root = new HBox();
        root.setStyle("-fx-background-color: " + AppConfig.COLOR_BG + ";");

        // Left branding panel
        VBox left = buildLeftPanel();
        left.setPrefWidth(420);

        // Right form panel
        VBox right = buildRightPanel();
        HBox.setHgrow(right, Priority.ALWAYS);

        root.getChildren().addAll(left, right);

        Scene scene = new Scene(root, AppConfig.WINDOW_DEF_WIDTH, AppConfig.WINDOW_DEF_HEIGHT);
        animateIn(right);
        return scene;
    }

    // ==================== LEFT PANEL ====================

    private VBox buildLeftPanel() {
        VBox panel = new VBox(20);
        panel.setAlignment(Pos.CENTER);
        panel.setPadding(new Insets(60, 50, 60, 50));
        panel.setStyle("-fx-background-color: " + AppConfig.COLOR_SECONDARY + ";");

        // Logo circle
        StackPane logoCircle = new StackPane();
        logoCircle.setPrefSize(80, 80);
        logoCircle.setStyle(
            "-fx-background-color: " + AppConfig.COLOR_PRIMARY + ";" +
            "-fx-background-radius: 40;"
        );
        Label logoIcon = new Label("⚕");
        logoIcon.setFont(Font.font(36));
        logoIcon.setTextFill(Color.WHITE);
        logoCircle.getChildren().add(logoIcon);

        Label appName = new Label(AppConfig.APP_NAME);
        appName.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 20));
        appName.setTextFill(Color.WHITE);
        appName.setTextAlignment(TextAlignment.CENTER);
        appName.setWrapText(true);

        Label subtitle = new Label(AppConfig.APP_SUBTITLE);
        subtitle.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 11));
        subtitle.setTextFill(Color.web("#90CAF9"));
        subtitle.setTextAlignment(TextAlignment.CENTER);
        subtitle.setWrapText(true);

        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: rgba(255,255,255,0.2);");
        sep.setMaxWidth(200);

        // Feature bullets
        String[] features = {
            "✓  Complete Inventory Management",
            "✓  Billing & Invoicing (POS)",
            "✓  Analytics & Reports",
            "✓  Multi-User Access Control",
            "✓  Dual-Database Storage"
        };
        VBox bullets = new VBox(10);
        bullets.setAlignment(Pos.CENTER_LEFT);
        for (String f : features) {
            Label lbl = new Label(f);
            lbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 11));
            lbl.setTextFill(Color.web("#B0BEC5"));
            bullets.getChildren().add(lbl);
        }

        Label version = new Label("Version " + AppConfig.APP_VERSION);
        version.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 9));
        version.setTextFill(Color.web("#546E7A"));

        panel.getChildren().addAll(logoCircle, appName, subtitle, sep, bullets, new Region(), version);
        VBox.setVgrow(new Region(), Priority.ALWAYS);
        return panel;
    }

    // ==================== RIGHT PANEL ====================

    private VBox buildRightPanel() {
        VBox panel = new VBox(0);
        panel.setAlignment(Pos.CENTER);
        panel.setStyle("-fx-background-color: white;");

        VBox form = new VBox(20);
        form.setAlignment(Pos.CENTER_LEFT);
        form.setPadding(new Insets(60, 70, 60, 70));
        form.setMaxWidth(460);

        Label welcome = new Label("Welcome Back");
        welcome.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 28));
        welcome.setTextFill(Color.web(AppConfig.COLOR_TEXT));

        Label subtext = new Label("Sign in to your pharmacy system");
        subtext.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 13));
        subtext.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));

        // Error banner
        errorLabel = new Label("");
        errorLabel.setFont(UIConstants.body());
        errorLabel.setTextFill(Color.web(AppConfig.COLOR_ERROR));
        errorLabel.setWrapText(true);
        errorLabel.setVisible(false);
        errorLabel.setStyle(
            "-fx-background-color: #FFEBEE; -fx-padding: 10 14; -fx-background-radius: 4;" +
            "-fx-border-color: " + AppConfig.COLOR_ERROR + "; -fx-border-width: 0 0 0 4; -fx-border-radius:4;"
        );
        errorLabel.setMaxWidth(Double.MAX_VALUE);

        // Username field
        VBox usernameBox = buildInputRow("Username", false);
        usernameField = (TextField) ((VBox) usernameBox).getChildren().get(1);

        // Password field
        VBox passwordBox = buildPasswordRow();

        // Remember me
        rememberMe = new CheckBox("Remember me");
        rememberMe.setFont(UIConstants.body());

        // Login button
        loginButton = UIFactory.createButton("Sign In", UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.LARGE);
        loginButton.setMaxWidth(Double.MAX_VALUE);
        loginButton.setOnAction(e -> handleLogin());

        // Enter key support
        usernameField.setOnAction(e -> passwordField.requestFocus());
        passwordField.setOnAction(e -> handleLogin());

        // Default credentials hint
        Label hint = new Label("Default: admin / Admin@123");
        hint.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 10));
        hint.setTextFill(Color.web("#90A4AE"));

        form.getChildren().addAll(
            welcome, subtext,
            new Region(),
            errorLabel,
            usernameBox,
            passwordBox,
            rememberMe,
            loginButton,
            hint
        );
        VBox.setMargin(errorLabel, new Insets(0));

        panel.getChildren().add(form);
        return panel;
    }

    private VBox buildInputRow(String label, boolean isPassword) {
        VBox box = new VBox(6);
        Label lbl = new Label(label);
        lbl.setFont(UIConstants.bodyBold());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));

        TextField field = UIFactory.createTextField(label);
        field.setPrefHeight(44);
        field.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(lbl, field);
        return box;
    }

    private VBox buildPasswordRow() {
        VBox box = new VBox(6);
        Label lbl = new Label("Password");
        lbl.setFont(UIConstants.bodyBold());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));

        passwordField = UIFactory.createPasswordField("Password");
        passwordField.setPrefHeight(44);
        passwordField.setMaxWidth(Double.MAX_VALUE);

        box.getChildren().addAll(lbl, passwordField);
        return box;
    }

    // ==================== LOGIN LOGIC ====================

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        errorLabel.setVisible(false);
        loginButton.setDisable(true);
        loginButton.setText("Signing in...");

        // Run off-FX thread to avoid UI freeze
        Thread loginThread = new Thread(() -> {
            try {
                AuthService auth = AppContext.getInstance().getAuthService();
                auth.login(username, password);

                Platform.runLater(() -> {
                    // Navigate to dashboard
                    MainController mainCtrl = new MainController(stage);
                    stage.setScene(mainCtrl.buildScene());
                    stage.setTitle(AppConfig.APP_NAME + " - Dashboard");
                });

            } catch (ValidationException ex) {
                Platform.runLater(() -> {
                    showError(ex.getMessage());
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                    passwordField.clear();
                });
            } catch (Exception ex) {
                Platform.runLater(() -> {
                    showError("System error: " + ex.getMessage());
                    loginButton.setDisable(false);
                    loginButton.setText("Sign In");
                });
            }
        });
        loginThread.setDaemon(true);
        loginThread.start();
    }

    private void showError(String msg) {
        errorLabel.setText("⚠  " + msg);
        errorLabel.setVisible(true);
        // Shake animation
        TranslateTransition shake = new TranslateTransition(Duration.millis(60), errorLabel);
        shake.setByX(8); shake.setCycleCount(6); shake.setAutoReverse(true); shake.play();
    }

    private void animateIn(javafx.scene.Node node) {
        FadeTransition ft = new FadeTransition(Duration.millis(500), node);
        ft.setFromValue(0); ft.setToValue(1);
        TranslateTransition tt = new TranslateTransition(Duration.millis(400), node);
        tt.setFromX(30); tt.setToX(0);
        new ParallelTransition(ft, tt).play();
    }
}
