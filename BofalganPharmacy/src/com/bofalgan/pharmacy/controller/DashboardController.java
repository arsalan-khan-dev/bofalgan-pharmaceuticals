package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.model.Alert;
import com.bofalgan.pharmacy.service.MedicineService;
import com.bofalgan.pharmacy.service.InvoiceService;
import com.bofalgan.pharmacy.ui.UIFactory;
import com.bofalgan.pharmacy.util.CurrencyFormatter;
import javafx.application.Platform;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.List;

public class DashboardController {

    private final AppContext      ctx;
    private final MainController  main;
    private VBox                  alertsBox;

    public DashboardController(AppContext ctx, MainController main) {
        this.ctx  = ctx;
        this.main = main;
    }

    public Node buildView() {
        VBox view = new VBox(0);
        view.setStyle("-fx-background-color: #F8FAFC;");

        // Top toolbar
        HBox toolbar = buildToolbar();
        view.getChildren().add(toolbar);

        // Scrollable content
        ScrollPane scroll = new ScrollPane();
        scroll.setFitToWidth(true);
        scroll.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scroll.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        VBox.setVgrow(scroll, Priority.ALWAYS);

        VBox content = new VBox(24);
        content.setPadding(new Insets(24, 28, 28, 28));

        content.getChildren().addAll(
            buildMetricCards(),
            buildAlertsSection(),
            buildQuickActions()
        );

        scroll.setContent(content);
        view.getChildren().add(scroll);

        // Load data off-thread
        loadDashboardData();

        return view;
    }

    // ==================== TOOLBAR ====================

    private HBox buildToolbar() {
        HBox bar = new HBox(12);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(16, 28, 16, 28));
        bar.setStyle(
            "-fx-background-color: white;" +
            "-fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;"
        );

        Label title = UIFactory.createH2("Dashboard");
        Label subtitle = new Label("Welcome back, " + ctx.getAuthService() != null &&
            com.bofalgan.pharmacy.service.SessionManager.getInstance().getCurrentUser() != null ?
            com.bofalgan.pharmacy.service.SessionManager.getInstance().getCurrentUser().getFullName() : "User");
        subtitle.setFont(UIConstants.body());
        subtitle.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button refreshBtn = UIFactory.createButton("↻ Refresh", UIFactory.ButtonType.SECONDARY);
        refreshBtn.setOnAction(e -> main.navigateTo("Dashboard"));

        Button runAlertsBtn = UIFactory.createButton("Run Alert Audit", UIFactory.ButtonType.WARNING);
        runAlertsBtn.setOnAction(e -> {
            ctx.getMedicineService().runAlertAudit();
            main.navigateTo("Dashboard");
        });

        bar.getChildren().addAll(title, subtitle, spacer, runAlertsBtn, refreshBtn);
        return bar;
    }

    // ==================== METRIC CARDS ====================

    private VBox metricSection;
    private Label totalMedLbl, lowStockLbl, expiringLbl, inventoryValueLbl, todayRevLbl, monthRevLbl;

    private FlowPane buildMetricCards() {
        FlowPane flow = new FlowPane(16, 16);
        flow.setStyle("-fx-background-color: transparent;");

        VBox totalMedCard = UIFactory.createMetricCard("Total Medicines", "...", "💊", AppConfig.COLOR_PRIMARY);
        totalMedLbl = (Label) ((HBox) totalMedCard.getChildren().get(0)).getChildren().get(1);
        totalMedCard.setOnMouseClicked(e -> main.navigateTo("Medicines"));

        VBox lowStockCard = UIFactory.createMetricCard("Low Stock", "...", "⚠️", AppConfig.COLOR_WARNING);
        lowStockLbl = (Label)((HBox) lowStockCard.getChildren().get(0)).getChildren().get(1);
        lowStockCard.setOnMouseClicked(e -> main.navigateTo("Medicines"));

        VBox expiringCard = UIFactory.createMetricCard("Expiring Soon", "...", "⏰", AppConfig.COLOR_ERROR);
        expiringLbl = (Label)((HBox) expiringCard.getChildren().get(0)).getChildren().get(1);
        expiringCard.setOnMouseClicked(e -> main.navigateTo("Medicines"));

        VBox valueCard = UIFactory.createMetricCard("Inventory Value", "...", "📦", "#9C27B0");
        inventoryValueLbl = (Label)((HBox) valueCard.getChildren().get(0)).getChildren().get(1);

        VBox todayRevCard = UIFactory.createMetricCard("Today Revenue", "...", "💵", AppConfig.COLOR_SUCCESS);
        todayRevLbl = (Label)((HBox) todayRevCard.getChildren().get(0)).getChildren().get(1);
        todayRevCard.setOnMouseClicked(e -> main.navigateTo("Invoices"));

        VBox monthRevCard = UIFactory.createMetricCard("Month Revenue", "...", "📈", "#2196F3");
        monthRevLbl = (Label)((HBox) monthRevCard.getChildren().get(0)).getChildren().get(1);
        monthRevCard.setOnMouseClicked(e -> main.navigateTo("Analytics"));

        flow.getChildren().addAll(totalMedCard, lowStockCard, expiringCard, valueCard, todayRevCard, monthRevCard);
        return flow;
    }

    // ==================== ALERTS ====================

    private VBox buildAlertsSection() {
        VBox section = new VBox(12);
        HBox header = UIFactory.createSectionHeader("Active Alerts",
            UIFactory.createButton("View All", UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.SMALL));

        alertsBox = new VBox(8);
        alertsBox.getChildren().add(new Label("Loading alerts..."));

        section.getChildren().addAll(header, alertsBox);
        return section;
    }

    // ==================== QUICK ACTIONS ====================

    private VBox buildQuickActions() {
        VBox section = new VBox(12);
        HBox header = UIFactory.createSectionHeader("Quick Actions");

        HBox actions = new HBox(12);
        actions.setAlignment(Pos.CENTER_LEFT);

        Button newSaleBtn    = UIFactory.createButton("🛒  New Sale",        UIFactory.ButtonType.PRIMARY, UIFactory.ButtonSize.LARGE);
        Button addMedBtn     = UIFactory.createButton("💊  Add Medicine",    UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
        Button newPurchaseBtn= UIFactory.createButton("📦  New Purchase",    UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);
        Button analyticsBtn  = UIFactory.createButton("📊  View Analytics",  UIFactory.ButtonType.SECONDARY, UIFactory.ButtonSize.LARGE);

        newSaleBtn.setOnAction(e     -> main.navigateTo("Billing"));
        addMedBtn.setOnAction(e      -> main.navigateTo("Medicines"));
        newPurchaseBtn.setOnAction(e -> main.navigateTo("Purchases"));
        analyticsBtn.setOnAction(e   -> main.navigateTo("Analytics"));

        actions.getChildren().addAll(newSaleBtn, addMedBtn, newPurchaseBtn, analyticsBtn);
        section.getChildren().addAll(header, actions);
        return section;
    }

    // ==================== ASYNC DATA LOAD ====================

    private void loadDashboardData() {
        Thread t = new Thread(() -> {
            try {
                MedicineService ms = ctx.getMedicineService();
                InvoiceService  is = ctx.getInvoiceService();

                int totalMed   = ms.getTotalMedicines();
                int lowStock   = ms.getLowStockCount();
                int expiring   = ms.getExpiringCount(AppConfig.EXPIRY_WARNING_DAYS);
                double invVal  = ms.getTotalInventoryValue();
                double todayRev= is.getTodayRevenue();
                double monthRev= is.getMonthRevenue();
                List<Alert> alerts = ctx.getAlertDAO().findActive();

                Platform.runLater(() -> {
                    updateMetricLabel(totalMedLbl, String.valueOf(totalMed));
                    updateMetricLabel(lowStockLbl, String.valueOf(lowStock));
                    updateMetricLabel(expiringLbl, String.valueOf(expiring));
                    updateMetricLabel(inventoryValueLbl, CurrencyFormatter.format(invVal));
                    updateMetricLabel(todayRevLbl, CurrencyFormatter.format(todayRev));
                    updateMetricLabel(monthRevLbl, CurrencyFormatter.format(monthRev));
                    renderAlerts(alerts);
                });
            } catch (Exception ex) {
                Platform.runLater(() -> System.err.println("[Dashboard] Load error: " + ex.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void updateMetricLabel(Label lbl, String value) {
        if (lbl != null) {
            // Walk up to find the VBox containing the HBox
            try {
                HBox hbox = (HBox) lbl.getParent();
                for (Node n : hbox.getChildren()) {
                    if (n instanceof VBox) {
                        VBox vb = (VBox) n;
                        for (Node child : vb.getChildren()) {
                            if (child instanceof Label && ((Label) child).getFont().getSize() > 18) {
                                ((Label) child).setText(value);
                                return;
                            }
                        }
                    }
                }
            } catch (Exception ignored) {}
            lbl.setText(value);
        }
    }

    private void renderAlerts(List<Alert> alerts) {
        alertsBox.getChildren().clear();
        if (alerts.isEmpty()) {
            Label none = new Label("✓  No active alerts. All systems normal.");
            none.setFont(UIConstants.body());
            none.setTextFill(Color.web(AppConfig.COLOR_SUCCESS));
            none.setStyle("-fx-background-color: #E8F5E9; -fx-padding: 12 16; -fx-background-radius: 6;");
            alertsBox.getChildren().add(none);
            return;
        }

        int shown = Math.min(alerts.size(), 8);
        for (int i = 0; i < shown; i++) {
            Alert a = alerts.get(i);
            String type = "CRITICAL".equals(a.getSeverity()) ? "ERROR" :
                          "WARNING".equals(a.getSeverity())  ? "WARNING" : "INFO";
            int alertId = a.getId();
            HBox banner = UIFactory.createAlertBanner(type, a.getMessage(), true, () -> {
                ctx.getAlertDAO().dismiss(alertId,
                    com.bofalgan.pharmacy.service.SessionManager.getInstance().getCurrentUser().getId());
                main.navigateTo("Dashboard");
            });
            alertsBox.getChildren().add(banner);
        }
        if (alerts.size() > shown) {
            Label more = new Label("... and " + (alerts.size() - shown) + " more alerts.");
            more.setFont(UIConstants.small());
            more.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));
            alertsBox.getChildren().add(more);
        }
    }
}
