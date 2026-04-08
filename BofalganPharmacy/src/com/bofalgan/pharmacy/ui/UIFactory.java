package com.bofalgan.pharmacy.ui;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.config.UIConstants;
import javafx.animation.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.*;
import javafx.util.Duration;

/**
 * Factory for all reusable UI components.
 * Pure JavaFX code — no FXML anywhere.
 */
public class UIFactory {

    // ==================== BUTTONS ====================

    public enum ButtonType { PRIMARY, SECONDARY, DANGER, SUCCESS, WARNING }
    public enum ButtonSize  { SMALL, MEDIUM, LARGE }

    public static Button createButton(String text, ButtonType type, ButtonSize size) {
        Button btn = new Button(text);
        applyButtonStyle(btn, type, size);
        return btn;
    }

    public static Button createButton(String text, ButtonType type) {
        return createButton(text, type, ButtonSize.MEDIUM);
    }

    public static Button createIconButton(String emoji, String tooltip, ButtonType type) {
        Button btn = new Button(emoji);
        btn.setTooltip(new Tooltip(tooltip));
        applyButtonStyle(btn, type, ButtonSize.SMALL);
        btn.setMinWidth(32);
        return btn;
    }

    private static void applyButtonStyle(Button btn, ButtonType type, ButtonSize size) {
        String bg, hover, text = "white";
        switch (type) {
            case PRIMARY:   bg = AppConfig.COLOR_PRIMARY;  hover = "#0097A7"; break;
            case DANGER:    bg = AppConfig.COLOR_ERROR;    hover = "#D32F2F"; break;
            case SUCCESS:   bg = AppConfig.COLOR_SUCCESS;  hover = "#388E3C"; break;
            case WARNING:   bg = AppConfig.COLOR_WARNING;  hover = "#F9A825"; text = "#333"; break;
            default:        bg = "#757575"; hover = "#616161"; break;
        }

        double h;
        double fontSize;
        double hPad;
        switch (size) {
            case SMALL:  h = UIConstants.BUTTON_HEIGHT_SM; fontSize = 11; hPad = 8;  break;
            case LARGE:  h = UIConstants.BUTTON_HEIGHT_LG; fontSize = 14; hPad = 20; break;
            default:     h = UIConstants.BUTTON_HEIGHT_MD; fontSize = 12; hPad = 14; break;
        }

        final String finalBg = bg, finalHover = hover, finalText = text;
        String baseStyle = String.format(
            "-fx-background-color: %s; -fx-text-fill: %s; -fx-font-size: %.0fpx;" +
            "-fx-padding: 0 %.0fpx; -fx-min-height: %.0fpx; -fx-cursor: hand;" +
            "-fx-background-radius: 6px; -fx-border-radius: 6px;",
            bg, text, fontSize, hPad, h
        );
        btn.setStyle(baseStyle);

        btn.setOnMouseEntered(e -> btn.setStyle(baseStyle.replace(finalBg, finalHover)
            + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.25), 6, 0, 0, 2);"));
        btn.setOnMouseExited(e -> btn.setStyle(baseStyle));
        btn.setOnMousePressed(e -> {
            btn.setScaleX(0.97); btn.setScaleY(0.97);
        });
        btn.setOnMouseReleased(e -> {
            btn.setScaleX(1.0); btn.setScaleY(1.0);
        });
    }

    // ==================== TEXT FIELDS ====================

    public static TextField createTextField(String prompt) {
        TextField tf = new TextField();
        tf.setPromptText(prompt);
        tf.setPrefHeight(UIConstants.INPUT_HEIGHT);
        tf.setStyle(
            "-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + ";" +
            "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 0 8px;" +
            "-fx-font-size: 12px;"
        );
        tf.focusedProperty().addListener((obs, old, focused) -> {
            if (focused) tf.setStyle(tf.getStyle().replace(AppConfig.COLOR_BORDER, AppConfig.COLOR_PRIMARY));
            else         tf.setStyle(tf.getStyle().replace(AppConfig.COLOR_PRIMARY, AppConfig.COLOR_BORDER));
        });
        return tf;
    }

    public static PasswordField createPasswordField(String prompt) {
        PasswordField pf = new PasswordField();
        pf.setPromptText(prompt);
        pf.setPrefHeight(UIConstants.INPUT_HEIGHT);
        pf.setStyle(
            "-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + ";" +
            "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-padding: 0 8px; -fx-font-size: 12px;"
        );
        return pf;
    }

    public static TextArea createTextArea(String prompt) {
        TextArea ta = new TextArea();
        ta.setPromptText(prompt);
        ta.setPrefRowCount(3);
        ta.setWrapText(true);
        ta.setStyle(
            "-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + ";" +
            "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 12px;"
        );
        return ta;
    }

    // ==================== LABELS ====================

    public static Label createLabel(String text, double fontSize, boolean bold) {
        Label lbl = new Label(text);
        lbl.setFont(bold ? UIConstants.bodyBold() : UIConstants.body());
        if (fontSize != 12) lbl.setFont(Font.font(UIConstants.FONT_FAMILY,
            bold ? FontWeight.BOLD : FontWeight.NORMAL, fontSize));
        return lbl;
    }

    public static Label createH1(String text) {
        Label lbl = new Label(text);
        lbl.setFont(UIConstants.h1());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));
        return lbl;
    }

    public static Label createH2(String text) {
        Label lbl = new Label(text);
        lbl.setFont(UIConstants.h2());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));
        return lbl;
    }

    public static Label createH3(String text) {
        Label lbl = new Label(text);
        lbl.setFont(UIConstants.h3());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));
        return lbl;
    }

    public static Label createSmall(String text, String color) {
        Label lbl = new Label(text);
        lbl.setFont(UIConstants.small());
        lbl.setTextFill(Color.web(color != null ? color : AppConfig.COLOR_TEXT_LIGHT));
        return lbl;
    }

    // ==================== METRIC CARD ====================

    public static VBox createMetricCard(String title, String value, String iconEmoji, String color) {
        VBox card = new VBox(6);
        card.setPrefSize(UIConstants.METRIC_CARD_WIDTH, UIConstants.METRIC_CARD_HEIGHT);
        card.setMinSize(UIConstants.METRIC_CARD_WIDTH, UIConstants.METRIC_CARD_HEIGHT);
        card.setPadding(new Insets(UIConstants.PAD_MD));
        card.setStyle(
            "-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + ";" +
            "-fx-border-radius: 8px; -fx-background-radius: 8px;" +
            "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"
        );
        card.setCursor(javafx.scene.Cursor.HAND);

        HBox top = new HBox();
        top.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label(iconEmoji);
        icon.setFont(Font.font(20));
        icon.setStyle("-fx-background-color: " + color + "22; -fx-background-radius: 20; -fx-padding: 6;");

        VBox right = new VBox(2);
        right.setPadding(new Insets(0, 0, 0, 10));
        Label titleLbl = new Label(title.toUpperCase());
        titleLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.NORMAL, 9));
        titleLbl.setTextFill(Color.web(AppConfig.COLOR_TEXT_LIGHT));
        Label valueLbl = new Label(value);
        valueLbl.setFont(Font.font(UIConstants.FONT_FAMILY, FontWeight.BOLD, 22));
        valueLbl.setTextFill(Color.web(color));
        right.getChildren().addAll(titleLbl, valueLbl);
        top.getChildren().addAll(icon, right);

        Region accent = new Region();
        accent.setPrefHeight(3);
        accent.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 2;");
        VBox.setMargin(accent, new Insets(4, 0, 0, 0));

        card.getChildren().addAll(top, accent);

        // Hover animation
        card.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(UIConstants.PAD_MD), card);
            st.setToX(1.04); st.setToY(1.04); st.play();
            card.setStyle(card.getStyle() +
                "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),12,0,0,4);");
        });
        card.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(UIConstants.PAD_MD), card);
            st.setToX(1.0); st.setToY(1.0); st.play();
            card.setStyle(card.getStyle()
                .replace("-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.18),12,0,0,4);",
                         "-fx-effect: dropshadow(gaussian,rgba(0,0,0,0.08),6,0,0,2);"));
        });

        return card;
    }

    // ==================== ALERT BANNER ====================

    public static HBox createAlertBanner(String type, String message, boolean dismissible, Runnable onDismiss) {
        String bg, border, icon;
        switch (type) {
            case "ERROR":    bg = "#FFEBEE"; border = AppConfig.COLOR_ERROR;   icon = "✖"; break;
            case "WARNING":  bg = "#FFF8E1"; border = AppConfig.COLOR_WARNING; icon = "⚠"; break;
            case "SUCCESS":  bg = "#E8F5E9"; border = AppConfig.COLOR_SUCCESS; icon = "✔"; break;
            default:         bg = "#E3F2FD"; border = "#2196F3";               icon = "ℹ"; break;
        }
        HBox banner = new HBox(10);
        banner.setAlignment(Pos.CENTER_LEFT);
        banner.setPadding(new Insets(10, 16, 10, 16));
        banner.setStyle("-fx-background-color: " + bg + "; -fx-border-color: " + border +
            "; -fx-border-width: 0 0 0 4; -fx-background-radius: 4; -fx-border-radius: 4;");

        Label iconLbl = new Label(icon);
        iconLbl.setTextFill(Color.web(border));
        Label msgLbl = new Label(message);
        msgLbl.setFont(UIConstants.body());
        msgLbl.setWrapText(true);
        HBox.setHgrow(msgLbl, Priority.ALWAYS);
        banner.getChildren().addAll(iconLbl, msgLbl);

        if (dismissible && onDismiss != null) {
            Button dismiss = new Button("✕");
            dismiss.setStyle("-fx-background-color: transparent; -fx-cursor: hand; -fx-font-size:12;");
            dismiss.setOnAction(e -> {
                FadeTransition ft = new FadeTransition(Duration.millis(300), banner);
                ft.setToValue(0);
                ft.setOnFinished(ev -> onDismiss.run());
                ft.play();
            });
            banner.getChildren().add(dismiss);
        }

        // Fade in
        banner.setOpacity(0);
        FadeTransition ft = new FadeTransition(Duration.millis(300), banner);
        ft.setToValue(1); ft.play();

        return banner;
    }

    // ==================== FORM FIELD ROW ====================

    /** Creates a label + control row for forms. */
    public static VBox createFormField(String label, javafx.scene.Node control) {
        VBox box = new VBox(4);
        Label lbl = new Label(label);
        lbl.setFont(UIConstants.bodyBold());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));
        box.getChildren().addAll(lbl, control);
        return box;
    }

    /** Creates a label + control row with error label. */
    public static VBox createFormFieldWithError(String label, javafx.scene.Node control, Label errorLabel) {
        VBox box = new VBox(3);
        Label lbl = new Label(label);
        lbl.setFont(UIConstants.bodyBold());
        lbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));
        errorLabel.setFont(UIConstants.small());
        errorLabel.setTextFill(Color.web(AppConfig.COLOR_ERROR));
        errorLabel.setVisible(false);
        box.getChildren().addAll(lbl, control, errorLabel);
        return box;
    }

    // ==================== SECTION HEADER ====================

    public static HBox createSectionHeader(String title, javafx.scene.Node... actions) {
        HBox header = new HBox(12);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 12, 0));

        Label titleLbl = new Label(title);
        titleLbl.setFont(UIConstants.h2());
        titleLbl.setTextFill(Color.web(AppConfig.COLOR_TEXT));

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        header.getChildren().add(titleLbl);
        header.getChildren().add(spacer);
        for (javafx.scene.Node action : actions) {
            header.getChildren().add(action);
        }
        return header;
    }

    // ==================== TABLE UTILS ====================

    public static <T> TableView<T> createStyledTable() {
        TableView<T> table = new TableView<>();
        table.setFixedCellSize(UIConstants.TABLE_ROW_HEIGHT);
        table.setStyle(
            "-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + ";" +
            "-fx-border-radius: 4; -fx-background-radius: 4;"
        );
        table.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        VBox.setVgrow(table, Priority.ALWAYS);
        return table;
    }

    // ==================== SEPARATOR ====================

    public static Separator createSeparator() {
        Separator sep = new Separator();
        sep.setStyle("-fx-background-color: " + AppConfig.COLOR_BORDER + ";");
        return sep;
    }

    // ==================== DIALOG ====================

    public static boolean showConfirmDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        ButtonType confirm = new ButtonType("Confirm", ButtonBar.ButtonData.OK_DONE);
        ButtonType cancel  = new ButtonType("Cancel",  ButtonBar.ButtonData.CANCEL_CLOSE);
        alert.getButtonTypes().setAll(confirm, cancel);
        return alert.showAndWait().map(b -> b == confirm).orElse(false);
    }

    public static void showErrorDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    public static void showInfoDialog(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title); alert.setHeaderText(null); alert.setContentText(message);
        alert.showAndWait();
    }

    // ==================== COMBOBOX STYLING ====================

    public static <T> ComboBox<T> createComboBox() {
        ComboBox<T> cb = new ComboBox<>();
        cb.setPrefHeight(UIConstants.INPUT_HEIGHT);
        cb.setMaxWidth(Double.MAX_VALUE);
        cb.setStyle(
            "-fx-background-color: white; -fx-border-color: " + AppConfig.COLOR_BORDER + ";" +
            "-fx-border-radius: 4px; -fx-background-radius: 4px; -fx-font-size: 12px;"
        );
        return cb;
    }

    // ==================== LOADING OVERLAY ====================

    public static StackPane createLoadingOverlay(String message) {
        StackPane overlay = new StackPane();
        overlay.setStyle("-fx-background-color: rgba(255,255,255,0.85);");

        VBox box = new VBox(12);
        box.setAlignment(Pos.CENTER);
        ProgressIndicator spinner = new ProgressIndicator();
        spinner.setPrefSize(48, 48);
        Label lbl = new Label(message);
        lbl.setFont(UIConstants.body());
        box.getChildren().addAll(spinner, lbl);
        overlay.getChildren().add(box);
        return overlay;
    }

    private UIFactory() {}
}
