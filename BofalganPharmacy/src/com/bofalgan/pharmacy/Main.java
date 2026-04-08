package com.bofalgan.pharmacy;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.controller.LoginController;
import com.bofalgan.pharmacy.util.DatabaseException;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Stage;

/**
 * Bofalgan Pharmaceuticals - Desktop Pharmacy Management System
 * Entry point for the JavaFX application.
 *
 * Startup sequence:
 *   1. Show splash/loading indicator
 *   2. Initialize MySQL connection (HikariCP)
 *   3. Run SchemaInitializer (create tables if not exist)
 *   4. Initialize JSON file storage
 *   5. Launch Login screen
 */
public class Main extends Application {

    private Stage primaryStage;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;

        stage.setTitle(AppConfig.APP_NAME);
        stage.setMinWidth(AppConfig.WINDOW_MIN_WIDTH);
        stage.setMinHeight(AppConfig.WINDOW_MIN_HEIGHT);
        stage.setWidth(AppConfig.WINDOW_DEF_WIDTH);
        stage.setHeight(AppConfig.WINDOW_DEF_HEIGHT);
        stage.centerOnScreen();

        // Show loading screen immediately
        stage.setScene(buildLoadingScene());
        stage.show();

        // Initialize system off the FX thread
        Thread initThread = new Thread(() -> {
            try {
                AppContext.initialize();

                Platform.runLater(() -> {
                    // System ready — show login
                    LoginController loginCtrl = new LoginController(stage);
                    stage.setScene(loginCtrl.buildScene());
                    stage.setTitle(AppConfig.APP_NAME + " - Login");
                });

            } catch (DatabaseException dbEx) {
                Platform.runLater(() -> showFatalError(
                    "Database Connection Failed",
                    "Could not connect to MySQL database.\n\n" +
                    "Please ensure:\n" +
                    "  1. MySQL Server is running on localhost:3306\n" +
                    "  2. Database 'bofalgan_pharmacy' exists\n" +
                    "  3. User 'bofalgan' has full access\n\n" +
                    "Error: " + dbEx.getMessage()
                ));
            } catch (Exception ex) {
                Platform.runLater(() -> showFatalError(
                    "Startup Error",
                    "System failed to initialize.\n\nError: " + ex.getMessage()
                ));
            }
        });
        initThread.setName("BofalganInit");
        initThread.setDaemon(true);
        initThread.start();

        // Graceful shutdown hook
        stage.setOnCloseRequest(e -> {
            AppContext.shutdown();
            Platform.exit();
        });
    }

    // ==================== LOADING SCREEN ====================

    private Scene buildLoadingScene() {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setStyle("-fx-background-color: " + AppConfig.COLOR_SECONDARY + ";");

        // Logo
        Label logo = new Label("⚕");
        logo.setFont(Font.font(56));
        logo.setTextFill(Color.web(AppConfig.COLOR_PRIMARY));

        Label appName = new Label(AppConfig.APP_NAME);
        appName.setFont(Font.font("Segoe UI", FontWeight.BOLD, 22));
        appName.setTextFill(Color.WHITE);

        Label subtitle = new Label(AppConfig.APP_SUBTITLE);
        subtitle.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        subtitle.setTextFill(Color.web("#90CAF9"));

        // Loading indicator (animated dots)
        Label loadingLbl = new Label("Initializing system...");
        loadingLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 11));
        loadingLbl.setTextFill(Color.web("#64748B"));

        javafx.scene.control.ProgressIndicator spinner = new javafx.scene.control.ProgressIndicator();
        spinner.setPrefSize(36, 36);
        spinner.setStyle("-fx-progress-color: " + AppConfig.COLOR_PRIMARY + ";");

        Label version = new Label("v" + AppConfig.APP_VERSION);
        version.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 9));
        version.setTextFill(Color.web("#334155"));
        VBox.setMargin(version, new Insets(30, 0, 0, 0));

        root.getChildren().addAll(logo, appName, subtitle, spinner, loadingLbl, version);

        return new Scene(root, AppConfig.WINDOW_DEF_WIDTH, AppConfig.WINDOW_DEF_HEIGHT);
    }

    // ==================== ERROR SCREEN ====================

    private void showFatalError(String title, String message) {
        VBox root = new VBox(20);
        root.setAlignment(Pos.CENTER);
        root.setPadding(new Insets(60));
        root.setStyle("-fx-background-color: #1A2238;");

        Label icon = new Label("⚠");
        icon.setFont(Font.font(48));
        icon.setTextFill(Color.web(AppConfig.COLOR_ERROR));

        Label titleLbl = new Label(title);
        titleLbl.setFont(Font.font("Segoe UI", FontWeight.BOLD, 20));
        titleLbl.setTextFill(Color.WHITE);

        Label msgLbl = new Label(message);
        msgLbl.setFont(Font.font("Segoe UI", FontWeight.NORMAL, 12));
        msgLbl.setTextFill(Color.web("#B0BEC5"));
        msgLbl.setWrapText(true);
        msgLbl.setMaxWidth(600);
        msgLbl.setStyle("-fx-background-color: #0F172A; -fx-padding: 16; -fx-background-radius: 8;");

        Button retryBtn = new Button("Retry");
        retryBtn.setStyle(
            "-fx-background-color: " + AppConfig.COLOR_PRIMARY + "; -fx-text-fill: white;" +
            "-fx-padding: 10 30; -fx-background-radius: 6; -fx-cursor: hand; -fx-font-size:13;"
        );
        retryBtn.setOnAction(e -> {
            primaryStage.setScene(buildLoadingScene());
            // Re-attempt init
            Thread t = new Thread(() -> {
                try {
                    AppContext.initialize();
                    Platform.runLater(() -> {
                        LoginController loginCtrl = new LoginController(primaryStage);
                        primaryStage.setScene(loginCtrl.buildScene());
                    });
                } catch (Exception ex2) {
                    Platform.runLater(() -> showFatalError(title, ex2.getMessage()));
                }
            });
            t.setDaemon(true);
            t.start();
        });

        Button exitBtn = new Button("Exit");
        exitBtn.setStyle(
            "-fx-background-color: transparent; -fx-text-fill: #90A4AE;" +
            "-fx-padding: 8 20; -fx-cursor: hand; -fx-font-size:12;" +
            "-fx-border-color: #90A4AE; -fx-border-radius: 6; -fx-background-radius:6;"
        );
        exitBtn.setOnAction(e -> Platform.exit());

        javafx.scene.layout.HBox btns = new javafx.scene.layout.HBox(12, retryBtn, exitBtn);
        btns.setAlignment(Pos.CENTER);

        root.getChildren().addAll(icon, titleLbl, msgLbl, btns);

        Scene errScene = new Scene(root, AppConfig.WINDOW_DEF_WIDTH, AppConfig.WINDOW_DEF_HEIGHT);
        primaryStage.setScene(errScene);
    }

    // ==================== ENTRY POINT ====================

    public static void main(String[] args) {
        launch(args);
    }
}
