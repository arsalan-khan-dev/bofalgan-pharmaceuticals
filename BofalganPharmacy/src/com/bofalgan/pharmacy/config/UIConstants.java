package com.bofalgan.pharmacy.config;

import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

/**
 * JavaFX UI styling constants. Font sizes, insets, border radii.
 */
public class UIConstants {

    // ==================== FONTS ====================
    public static final String FONT_FAMILY    = "Segoe UI";
    public static final double FONT_H1        = 24;
    public static final double FONT_H2        = 18;
    public static final double FONT_H3        = 15;
    public static final double FONT_BODY      = 12;
    public static final double FONT_SMALL     = 10;

    public static Font h1() { return Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_H1); }
    public static Font h2() { return Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_H2); }
    public static Font h3() { return Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_H3); }
    public static Font body() { return Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_BODY); }
    public static Font bodyBold() { return Font.font(FONT_FAMILY, FontWeight.BOLD, FONT_BODY); }
    public static Font small() { return Font.font(FONT_FAMILY, FontWeight.NORMAL, FONT_SMALL); }

    // ==================== SPACING ====================
    public static final double PAD_XS  = 4;
    public static final double PAD_SM  = 8;
    public static final double PAD_MD  = 16;
    public static final double PAD_LG  = 24;
    public static final double PAD_XL  = 32;

    // ==================== COMPONENT SIZES ====================
    public static final double SIDEBAR_WIDTH         = 220;
    public static final double SIDEBAR_COLLAPSED     = 60;
    public static final double TOOLBAR_HEIGHT        = 60;
    public static final double STATUSBAR_HEIGHT      = 28;
    public static final double METRIC_CARD_WIDTH     = 200;
    public static final double METRIC_CARD_HEIGHT    = 110;
    public static final double BUTTON_HEIGHT_SM      = 28;
    public static final double BUTTON_HEIGHT_MD      = 36;
    public static final double BUTTON_HEIGHT_LG      = 44;
    public static final double BORDER_RADIUS         = 8;
    public static final double INPUT_HEIGHT          = 34;

    // ==================== TABLE ====================
    public static final double TABLE_ROW_HEIGHT  = 40;

    private UIConstants() {}
}
