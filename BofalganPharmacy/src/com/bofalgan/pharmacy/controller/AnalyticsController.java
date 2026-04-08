package com.bofalgan.pharmacy.controller;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.AppContext;
import com.bofalgan.pharmacy.config.UIConstants;
import com.bofalgan.pharmacy.dao.AnalyticsDAO;
import com.bofalgan.pharmacy.service.SessionManager;
import com.bofalgan.pharmacy.ui.UIFactory;
import com.bofalgan.pharmacy.util.CurrencyFormatter;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.geometry.*;
import javafx.scene.Node;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.*;

import java.util.List;
import java.util.Map;

public class AnalyticsController {

    private final AppContext     ctx;
    private final MainController main;
    private TabPane              tabPane;

    public AnalyticsController(AppContext ctx, MainController main) {
        this.ctx  = ctx;
        this.main = main;
    }

    public Node buildView() {
        VBox view = new VBox(0);
        view.setStyle("-fx-background-color: #F8FAFC;");

        view.getChildren().addAll(buildToolbar(), buildTabs());
        return view;
    }

    private HBox buildToolbar() {
        HBox bar = new HBox(10);
        bar.setAlignment(Pos.CENTER_LEFT);
        bar.setPadding(new Insets(14, 24, 14, 24));
        bar.setStyle("-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + "; -fx-border-width: 0 0 1 0;");

        Label title = UIFactory.createH2("Analytics Dashboard");
        Region spacer = new Region(); HBox.setHgrow(spacer, Priority.ALWAYS);
        Button refreshBtn = UIFactory.createButton("↻ Refresh Data", UIFactory.ButtonType.SECONDARY);
        refreshBtn.setOnAction(e -> refreshAllCharts());

        bar.getChildren().addAll(title, spacer, refreshBtn);
        return bar;
    }

    private TabPane buildTabs() {
        tabPane = new TabPane();
        tabPane.setStyle("-fx-background-color: #F8FAFC;");
        tabPane.setTabClosingPolicy(TabPane.TabClosingPolicy.UNAVAILABLE);
        VBox.setVgrow(tabPane, Priority.ALWAYS);

        tabPane.getTabs().addAll(
            buildSalesTab(),
            buildTopMedicinesTab(),
            buildInventoryTab(),
            buildExpiryTab(),
            buildSupplierTab()
        );
        tabPane.getSelectionModel().selectedIndexProperty().addListener((obs, old, idx) -> loadTabData(idx.intValue()));
        loadTabData(0);
        return tabPane;
    }

    // ==================== TAB 1: SALES ====================

    private LineChart<String, Number> dailyRevenueChart;
    private BarChart<String, Number>  monthlyRevenueChart;
    private PieChart paymentMethodChart;

    private Tab buildSalesTab() {
        Tab tab = new Tab("💰 Sales");
        ScrollPane sp = new ScrollPane();
        sp.setFitToWidth(true);
        sp.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        VBox content = new VBox(20);
        content.setPadding(new Insets(20));

        // Daily revenue line chart
        CategoryAxis xAxis1 = new CategoryAxis();
        NumberAxis   yAxis1 = new NumberAxis();
        xAxis1.setLabel("Date"); yAxis1.setLabel("Revenue ($)");
        dailyRevenueChart = new LineChart<>(xAxis1, yAxis1);
        dailyRevenueChart.setTitle("Daily Revenue (Last 30 Days)");
        dailyRevenueChart.setPrefHeight(300);
        dailyRevenueChart.setCreateSymbols(true);
        styleChart(dailyRevenueChart);

        // Monthly revenue bar chart
        CategoryAxis xAxis2 = new CategoryAxis();
        NumberAxis   yAxis2 = new NumberAxis();
        xAxis2.setLabel("Month"); yAxis2.setLabel("Revenue ($)");
        monthlyRevenueChart = new BarChart<>(xAxis2, yAxis2);
        monthlyRevenueChart.setTitle("Monthly Revenue (Last 12 Months)");
        monthlyRevenueChart.setPrefHeight(280);
        styleChart(monthlyRevenueChart);

        // Payment method pie chart
        paymentMethodChart = new PieChart();
        paymentMethodChart.setTitle("Revenue by Payment Method");
        paymentMethodChart.setPrefHeight(280);
        paymentMethodChart.setLegendSide(Side.RIGHT);

        HBox row2 = new HBox(16);
        VBox.setVgrow(monthlyRevenueChart, Priority.ALWAYS);
        HBox.setHgrow(monthlyRevenueChart, Priority.ALWAYS);
        HBox.setHgrow(paymentMethodChart,  Priority.ALWAYS);
        row2.getChildren().addAll(monthlyRevenueChart, paymentMethodChart);

        content.getChildren().addAll(dailyRevenueChart, row2);
        sp.setContent(content);
        tab.setContent(sp);
        return tab;
    }

    // ==================== TAB 2: TOP MEDICINES ====================

    private BarChart<Number, String> topMedicinesChart;

    private Tab buildTopMedicinesTab() {
        Tab tab = new Tab("🏆 Top Medicines");
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        NumberAxis  xAxis = new NumberAxis();
        CategoryAxis yAxis = new CategoryAxis();
        xAxis.setLabel("Units Sold"); yAxis.setLabel("Medicine");
        topMedicinesChart = new BarChart<>(xAxis, yAxis);
        topMedicinesChart.setTitle("Top 10 Selling Medicines (Last 30 Days)");
        topMedicinesChart.setPrefHeight(400);
        topMedicinesChart.setBarGap(2);
        topMedicinesChart.setCategoryGap(8);
        styleChart(topMedicinesChart);
        VBox.setVgrow(topMedicinesChart, Priority.ALWAYS);

        content.getChildren().add(topMedicinesChart);
        tab.setContent(content);
        return tab;
    }

    // ==================== TAB 3: INVENTORY ====================

    private BarChart<String, Number> stockByCategoryChart;

    private Tab buildInventoryTab() {
        Tab tab = new Tab("📦 Inventory");
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Category"); yAxis.setLabel("Total Quantity");
        stockByCategoryChart = new BarChart<>(xAxis, yAxis);
        stockByCategoryChart.setTitle("Stock Quantity by Category");
        stockByCategoryChart.setPrefHeight(380);
        styleChart(stockByCategoryChart);
        VBox.setVgrow(stockByCategoryChart, Priority.ALWAYS);

        content.getChildren().add(stockByCategoryChart);
        tab.setContent(content);
        return tab;
    }

    // ==================== TAB 4: EXPIRY ====================

    private PieChart expiryStatusChart;
    private BarChart<String, Number> expiryByMonthChart;

    private Tab buildExpiryTab() {
        Tab tab = new Tab("⏰ Expiry Analysis");
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        expiryStatusChart = new PieChart();
        expiryStatusChart.setTitle("Medicines by Expiry Status");
        expiryStatusChart.setPrefHeight(280);
        expiryStatusChart.setLegendSide(Side.RIGHT);

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Month"); yAxis.setLabel("Count");
        expiryByMonthChart = new BarChart<>(xAxis, yAxis);
        expiryByMonthChart.setTitle("Medicines Expiring by Month (Next 6 Months)");
        expiryByMonthChart.setPrefHeight(280);
        styleChart(expiryByMonthChart);

        HBox row = new HBox(16);
        HBox.setHgrow(expiryStatusChart,  Priority.ALWAYS);
        HBox.setHgrow(expiryByMonthChart, Priority.ALWAYS);
        row.getChildren().addAll(expiryStatusChart, expiryByMonthChart);

        content.getChildren().add(row);
        tab.setContent(content);
        return tab;
    }

    // ==================== TAB 5: SUPPLIERS ====================

    private BarChart<String, Number>  supplierPurchaseChart;

    private Tab buildSupplierTab() {
        Tab tab = new Tab("🏭 Supplier Performance");
        VBox content = new VBox(16);
        content.setPadding(new Insets(20));

        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis   yAxis = new NumberAxis();
        xAxis.setLabel("Supplier"); yAxis.setLabel("Total Purchased ($)");
        supplierPurchaseChart = new BarChart<>(xAxis, yAxis);
        supplierPurchaseChart.setTitle("Total Purchases by Supplier");
        supplierPurchaseChart.setPrefHeight(340);
        styleChart(supplierPurchaseChart);

        content.getChildren().add(supplierPurchaseChart);
        tab.setContent(content);
        return tab;
    }

    // ==================== DATA LOADING ====================

    private void loadTabData(int tabIndex) {
        AnalyticsDAO dao = ctx.getAnalyticsDAO();
        Thread t = new Thread(() -> {
            try {
                switch (tabIndex) {
                    case 0 -> loadSalesData(dao);
                    case 1 -> loadTopMedicinesData(dao);
                    case 2 -> loadInventoryData(dao);
                    case 3 -> loadExpiryData(dao);
                    case 4 -> loadSupplierData(dao);
                }
            } catch (Exception ex) {
                Platform.runLater(() -> System.err.println("[Analytics] " + ex.getMessage()));
            }
        });
        t.setDaemon(true);
        t.start();
    }

    private void loadSalesData(AnalyticsDAO dao) {
        Map<String, Double> daily   = dao.getDailyRevenue(30);
        Map<String, Double> monthly = dao.getMonthlyRevenue(12);
        Map<String, Double> byMethod = dao.getRevenueByPaymentMethod();

        Platform.runLater(() -> {
            // Daily line chart
            XYChart.Series<String, Number> series1 = new XYChart.Series<>();
            series1.setName("Revenue");
            daily.forEach((k, v) -> series1.getData().add(new XYChart.Data<>(k, v)));
            dailyRevenueChart.getData().setAll(series1);

            // Monthly bar chart
            XYChart.Series<String, Number> series2 = new XYChart.Series<>();
            series2.setName("Revenue");
            monthly.forEach((k, v) -> series2.getData().add(new XYChart.Data<>(k, v)));
            monthlyRevenueChart.getData().setAll(series2);

            // Payment pie
            paymentMethodChart.getData().setAll(
                byMethod.entrySet().stream()
                    .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                    .toList()
            );
        });
    }

    private void loadTopMedicinesData(AnalyticsDAO dao) {
        List<Object[]> top = dao.getTopSellingMedicines(10, 30);
        Platform.runLater(() -> {
            XYChart.Series<Number, String> series = new XYChart.Series<>();
            series.setName("Units Sold");
            for (Object[] row : top) {
                series.getData().add(new XYChart.Data<>((int) row[1], (String) row[0]));
            }
            topMedicinesChart.getData().setAll(series);
        });
    }

    private void loadInventoryData(AnalyticsDAO dao) {
        Map<String, Integer> byCategory = dao.getStockByCategory();
        Platform.runLater(() -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Qty in Stock");
            byCategory.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
            stockByCategoryChart.getData().setAll(series);
        });
    }

    private void loadExpiryData(AnalyticsDAO dao) {
        Map<String, Integer> statusCounts = dao.getExpiryStatusCounts();
        Map<String, Integer> byMonth      = dao.getExpiryByMonth();
        Platform.runLater(() -> {
            expiryStatusChart.getData().setAll(
                statusCounts.entrySet().stream()
                    .filter(e -> e.getValue() > 0)
                    .map(e -> new PieChart.Data(e.getKey(), e.getValue()))
                    .toList()
            );
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Expiring");
            byMonth.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
            expiryByMonthChart.getData().setAll(series);
        });
    }

    private void loadSupplierData(AnalyticsDAO dao) {
        Map<String, Double> bySupplier = dao.getPurchaseBySupplier();
        Platform.runLater(() -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Purchased ($)");
            bySupplier.forEach((k, v) -> series.getData().add(new XYChart.Data<>(k, v)));
            supplierPurchaseChart.getData().setAll(series);
        });
    }

    private void refreshAllCharts() {
        for (int i = 0; i < 5; i++) loadTabData(i);
    }

    private void styleChart(Chart chart) {
        chart.setStyle("-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER +
            "; -fx-border-radius: 8; -fx-background-radius: 8;");
        chart.setAnimated(true);
    }
}
