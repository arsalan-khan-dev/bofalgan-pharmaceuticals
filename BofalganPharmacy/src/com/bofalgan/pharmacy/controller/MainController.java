package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.dao.AlertDAO;
import com.bofalgan.pharmacy.service.SessionManager;
import com.bofalgan.pharmacy.ui.UIFactory;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Root controller — sidebar + main content area.
 * All screen switching happens here.
 */
public class MainController {

    private final Stage   stage;
    private BorderPane    root;
    private VBox          sidebar;
    private StackPane     contentArea;
    private Label         statusDbLabel;
    private Label         clockLabel;
    private Label         alertBadge;
    private boolean       sidebarExpanded = true;
    private String        activeSection   = "Dashboard";

    // Nav item definitions: label, emoji icon, permission
    private static final Object[][] NAV_ITEMS = {
        {"Dashboard",  "🏠", null},
        {"Medicines",  "💊", null},
        {"Billing",    "🛒", "CREATE_INVOICE"},
        {"Purchases",  "📦", "ADD_PURCHASE"},
        {"Suppliers",  "🏭", "ADD_SUPPLIER"},
        {"Invoices",   "🧾", null},
        {"Analytics",  "📊", "VIEW_ANALYTICS"},
        {"Reports",    "📋", "VIEW_REPORTS"},
        {"Users",      "👥", "MANAGE_USERS"},
        {"Settings",   "⚙️",  "MANAGE_SETTINGS"},
    };

    public MainController(Stage stage) {
        this.stage = stage;
    }

    public Scene buildScene() {
        root = new BorderPane();
        root.setStyle("-fx-background-color: " + AppConfig.COLOR_BG + ";");

        // Build sidebar
        sidebar = buildSidebar();
        root.setLeft(sidebar);

        // Content area
        contentArea = new StackPane();
        contentArea.setStyle("-fx-background-color: #F8FAFC;");
        root.setCenter(contentArea);

        // Status bar
        root.setBottom(buildStatusBar());

        // Load dashboard by default
        navigateTo("Dashboard");

        Scene scene = new Scene(root, AppConfig.WINDOW_DEF_WIDTH, AppConfig.WINDOW_DEF_HEIGHT);
        startClock();
        return scene;
    }

    // ==================== SIDEBAR ====================

    private VBox buildSidebar() {
        VBox sb = new VBox(0);
        sb.setPrefWidth(UIConstants.SIDEBAR_WIDTH);
        sb.setMinWidth(UIConstants.SIDEBAR_WIDTH);
        sb.setStyle("-fx-background-color: " + AppConfig.COLOR_SIDEBAR_BG + ";");

        // Header (toggle + title)
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(18, 14, 18, 14));
        header.setStyle("-fx-background-color: #111827;");

        Button toggleBtn = new Button("☰");
        toggleBtn.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-size:16; -fx-cursor:hand;");
        toggleBtn.setOnAction(e -> toggleSidebar());

        Label appLbl = new Label(AppConfig.APP_NAME);
        appLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 11));
        appLbl.setTextFill(Color.WHITE);
        appLbl.setWrapText(true);
        appLbl.setMaxWidth(160);

        header.getChildren().addAll(toggleBtn, appLbl);

        // User info strip
        HBox userStrip = new HBox(8);
        userStrip.setAlignment(Pos.CENTER_LEFT);
        userStrip.setPadding(new Insets(10, 14, 10, 14));
        userStrip.setStyle("-fx-background-color: #0F172A;");

        StackPane avatar = new StackPane();
        avatar.setPrefSize(32, 32);
        avatar.setStyle("-fx-background-color: " + AppConfig.COLOR_PRIMARY + "; -fx-background-radius:16;");
        Label avatarLbl = new Label(getInitials());
        avatarLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 11));
        avatarLbl.setTextFill(Color.WHITE);
        avatar.getChildren().add(avatarLbl);

        VBox userInfo = new VBox(1);
        Label nameLbl = new Label(SessionManager.getInstance().getCurrentUser().getFullName());
        nameLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 11));
        nameLbl.setTextFill(Color.WHITE);
        Label roleLbl = new Label(SessionManager.getInstance().getCurrentRole());
        roleLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 9));
        roleLbl.setTextFill(Color.web("#64748B"));
        userInfo.getChildren().addAll(nameLbl, roleLbl);
        userStrip.getChildren().addAll(avatar, userInfo);

        // Nav items
        VBox navItems = new VBox(2);
        navItems.setPadding(new Insets(10, 0, 10, 0));

        SessionManager session = SessionManager.getInstance();
        for (Object[] item : NAV_ITEMS) {
            String label      = (String) item[0];
            String icon       = (String) item[1];
            String permission = (String) item[2];

            if (permission != null && !session.hasPermission(permission)) continue;

            HBox navItem = buildNavItem(label, icon);
            navItem.setOnMouseClicked(e -> navigateTo(label));
            navItems.getChildren().add(navItem);
        }

        // Spacer
        Region spacer = new Region();
        VBox.setVgrow(spacer, Priority.ALWAYS);

        // Logout
        HBox logoutItem = buildNavItem("Logout", "🚪");
        logoutItem.setStyle(logoutItem.getStyle() + "-fx-background-color: #1F0000;");
        logoutItem.setOnMouseClicked(e -> handleLogout());

        sb.getChildren().addAll(header, userStrip, navItems, spacer, logoutItem);
        return sb;
    }

    private HBox buildNavItem(String label, String icon) {
        HBox item = new HBox(12);
        item.setAlignment(Pos.CENTER_LEFT);
        item.setPadding(new Insets(11, 14, 11, 18));
        item.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");

        Label iconLbl = new Label(icon);
        iconLbl.setFont(Font.font(15));

        Label textLbl = new Label(label);
        textLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 12));
        textLbl.setTextFill(Color.web("#CBD5E1"));

        if (label.equals(activeSection)) {
            item.setStyle("-fx-background-color: " + AppConfig.COLOR_PRIMARY + "22; -fx-cursor:hand;" +
                "-fx-border-color: " + AppConfig.COLOR_PRIMARY + "; -fx-border-width:0 0 0 3;");
            textLbl.setTextFill(Color.web(AppConfig.COLOR_PRIMARY));
            textLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 12));
        }

        // Alert badge placeholder for "Dashboard"/"Medicines"
        if (label.equals("Dashboard")) {
            alertBadge = new Label("0");
            alertBadge.setStyle(
                "-fx-background-color: " + AppConfig.COLOR_ERROR + "; -fx-text-fill: white;" +
                "-fx-background-radius: 9; -fx-font-size: 9; -fx-padding: 1 5; -fx-min-width: 18;"
            );
            alertBadge.setVisible(false);
            HBox.setMargin(alertBadge, new Insets(0, 0, 0, 6));
            item.getChildren().addAll(iconLbl, textLbl, alertBadge);
        } else {
            item.getChildren().addAll(iconLbl, textLbl);
        }

        item.setOnMouseEntered(e -> {
            if (!label.equals(activeSection))
                item.setStyle("-fx-background-color: rgba(255,255,255,0.06); -fx-cursor:hand;");
        });
        item.setOnMouseExited(e -> {
            if (!label.equals(activeSection))
                item.setStyle("-fx-background-color: transparent; -fx-cursor:hand;");
        });

        return item;
    }

    private void toggleSidebar() {
        double targetWidth = sidebarExpanded ? UIConstants.SIDEBAR_COLLAPSED : UIConstants.SIDEBAR_WIDTH;
        Timeline tl = new Timeline(
            new KeyFrame(Duration.millis(UIConstants.PAD_XL),
                new KeyValue(sidebar.prefWidthProperty(), targetWidth, Interpolator.EASE_BOTH),
                new KeyValue(sidebar.minWidthProperty(), targetWidth, Interpolator.EASE_BOTH))
        );
        tl.play();
        sidebarExpanded = !sidebarExpanded;
    }

    // ==================== STATUS BAR ====================

    private HBox buildStatusBar() {
        HBox bar = new HBox(20);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(4, 16, 4, 16));
        bar.setPrefHeight(UIConstants.STATUSBAR_HEIGHT);
        bar.setStyle("-fx-background-color: #1E293B;");

        // DB status
        HBox dbStatus = new HBox(6);
        dbStatus.setAlignment(Pos.CENTER_LEFT);
        Label dbDot = new Label("●");
        dbDot.setTextFill(AppContext.getInstance() != null ? Color.web(AppConfig.COLOR_SUCCESS) : Color.web(AppConfig.COLOR_ERROR));
        dbDot.setFont(Font.font(10));
        statusDbLabel = new Label("MySQL Connected");
        statusDbLabel.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 10));
        statusDbLabel.setTextFill(Color.web("#94A3B8"));
        dbStatus.getChildren().addAll(dbDot, statusDbLabel);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Logged in user
        Label userLbl = new Label("👤 " + SessionManager.getInstance().getCurrentUser().getFullName() +
            " | " + SessionManager.getInstance().getCurrentRole());
        userLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 10));
        userLbl.setTextFill(Color.web("#94A3B8"));

        Region spacer2 = new Region();
        HBox.setHgrow(spacer2, Priority.ALWAYS);

        clockLabel = new Label();
        clockLabel.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 10));
        clockLabel.setTextFill(Color.web("#94A3B8"));

        bar.getChildren().addAll(dbStatus, spacer, userLbl, spacer2, clockLabel);
        return bar;
    }

    // ==================== NAVIGATION ====================

    public void navigateTo(String section) {
        activeSection = section;

        // Rebuild sidebar to reflect new active
        sidebar.getChildren().clear();

        VBox newSidebar = buildSidebar();
        sidebar.getChildren().addAll(newSidebar.getChildren());

        // Load screen with fade transition
        javafx.scene.Node screen = buildScreen(section);
        if (screen == null) return;

        FadeTransition ft = new FadeTransition(Duration.millis(AppConfig.ANIM_TRANSITION), screen);
        ft.setFromValue(0); ft.setToValue(1);

        contentArea.getChildren().setAll(screen);
        ft.play();
    }

    private javafx.scene.Node buildScreen(String section) {
        AppContext ctx = AppContext.getInstance();
        switch (section) {
            case "Dashboard":  return new DashboardController(ctx, this).buildView();
            case "Medicines":  return new MedicineController(ctx, this).buildView();
            case "Billing":    return new BillingController(ctx, this).buildView();
            case "Purchases":  return new PurchaseController(ctx, this).buildView();
            case "Suppliers":  return new SupplierController(ctx, this).buildView();
            case "Invoices":   return new InvoiceController(ctx, this).buildView();
            case "Analytics":  return new AnalyticsController(ctx, this).buildView();
            case "Reports":    return new ReportsController(ctx, this).buildView();
            case "Users":      return new UserManagementController(ctx, this).buildView();
            case "Settings":   return new SettingsController(ctx, this).buildView();
            default:           return new DashboardController(ctx, this).buildView();
        }
    }

    // ==================== LOGOUT ====================

    private void handleLogout() {
        boolean confirm = UIFactory.showConfirmDialog("Logout", "Are you sure you want to logout?");
        if (!confirm) return;
        AppContext.getInstance().getAuthService().logout();
        LoginController loginCtrl = new LoginController(stage);
        stage.setScene(loginCtrl.buildScene());
        stage.setTitle(AppConfig.APP_NAME + " - Login");
    }

    // ==================== CLOCK ====================

    private void startClock() {
        Timeline clock = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            clockLabel.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd-MMM-yyyy  HH:mm:ss")));
            refreshAlertBadge();
        }));
        clock.setCycleCount(Animation.INDEFINITE);
        clock.play();
    }

    private void refreshAlertBadge() {
        try {
            int count = AppContext.getInstance().getAlertDAO().countActive();
            if (alertBadge != null) {
                alertBadge.setText(String.valueOf(count));
                alertBadge.setVisible(count > 0);
            }
        } catch (Exception ignored) {}
    }

    private String getInitials() {
        String name = SessionManager.getInstance().getCurrentUser().getFullName();
        if (name == null || name.isBlank()) return "U";
        String[] parts = name.trim().split("\\s+");
        if (parts.length >= 2) return "" + parts[0].charAt(0) + parts[1].charAt(0);
        return name.substring(0, Math.min(2, name.length())).toUpperCase();
    }
}
