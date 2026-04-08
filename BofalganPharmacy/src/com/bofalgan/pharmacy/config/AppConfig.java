package com.bofalgan.pharmacy.config;

/**
 * Central configuration for Bofalgan Pharmaceuticals.
 * All constants, defaults, and runtime settings live here.
 */
public class AppConfig {

    // ==================== APPLICATION INFO ====================
    public static final String APP_NAME        = "Bofalgan Pharmaceuticals";
    public static final String APP_VERSION     = "1.0.0";
    public static final String APP_SUBTITLE    = "Professional Pharmacy Management System";

    // ==================== WINDOW ====================
    public static final double WINDOW_MIN_WIDTH  = 1100;
    public static final double WINDOW_MIN_HEIGHT = 700;
    public static final double WINDOW_DEF_WIDTH  = 1280;
    public static final double WINDOW_DEF_HEIGHT = 800;

    // ==================== MYSQL DATABASE ====================
    public static final String DB_HOST     = "localhost";
    public static final int    DB_PORT     = 3306;
    public static final String DB_NAME     = "bofalgan_pharmacy";
    public static final String DB_USER     = "bofalgan";
    public static final String DB_PASSWORD = "BofalganDB@2024";
    public static final String DB_URL      =
        "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME
        + "?useSSL=false&serverTimezone=UTC&allowPublicKeyRetrieval=true"
        + "&useUnicode=true&characterEncoding=UTF-8";

    // HikariCP pool settings
    public static final int DB_POOL_SIZE    = 10;
    public static final int DB_POOL_MIN     = 2;
    public static final long DB_POOL_TIMEOUT = 30000;

    // ==================== FILE STORAGE ====================
    public static final String DATA_DIR            = "data";
    public static final String MEDICINES_FILE      = DATA_DIR + "/medicines.json";
    public static final String SUPPLIERS_FILE      = DATA_DIR + "/suppliers.json";
    public static final String USERS_FILE          = DATA_DIR + "/users.json";
    public static final String INVOICES_FILE       = DATA_DIR + "/invoices.json";
    public static final String PURCHASES_FILE      = DATA_DIR + "/purchases.json";
    public static final String ALERTS_FILE         = DATA_DIR + "/alerts.json";
    public static final String SETTINGS_FILE       = DATA_DIR + "/settings.json";
    public static final String SYNC_LOG_FILE       = DATA_DIR + "/sync_log.json";
    public static final long   SYNC_INTERVAL_MS    = 5 * 60 * 1000; // 5 minutes

    // ==================== SECURITY ====================
    public static final int  BCRYPT_ROUNDS             = 12;
    public static final int  MAX_FAILED_LOGINS         = 3;
    public static final int  LOCK_DURATION_MINUTES     = 15;
    public static final int  SESSION_TIMEOUT_MINUTES   = 30;

    // ==================== INVENTORY ====================
    public static final int  DEFAULT_REORDER_LEVEL     = 10;
    public static final int  EXPIRY_WARNING_DAYS       = 30;
    public static final int  EXPIRY_CRITICAL_DAYS      = 7;
    public static final int  PAGE_SIZE                 = 50;

    // ==================== INVOICE ====================
    public static final String INVOICE_PREFIX   = "INV";
    public static final String CURRENCY_SYMBOL  = "$";
    public static final double DEFAULT_TAX_RATE = 0.0;

    // ==================== COLORS (CSS-style hex strings) ====================
    public static final String COLOR_PRIMARY     = "#00BCD4";
    public static final String COLOR_SECONDARY   = "#1A2238";
    public static final String COLOR_BG          = "#FFFFFF";
    public static final String COLOR_WARNING     = "#FFC107";
    public static final String COLOR_ERROR       = "#F44336";
    public static final String COLOR_SUCCESS     = "#4CAF50";
    public static final String COLOR_TEXT        = "#333333";
    public static final String COLOR_TEXT_LIGHT  = "#666666";
    public static final String COLOR_BORDER      = "#CCCCCC";
    public static final String COLOR_ROW_ALT     = "#F5F5F5";
    public static final String COLOR_SIDEBAR_BG  = "#1A2238";
    public static final String COLOR_SIDEBAR_TXT = "#FFFFFF";

    // ==================== ANIMATIONS ====================
    public static final int ANIM_BUTTON_HOVER  = 150;
    public static final int ANIM_CARD_HOVER    = 200;
    public static final int ANIM_TRANSITION    = 300;
    public static final int ANIM_ALERT_FADE    = 300;

    // ==================== BACKUP ====================
    public static final String BACKUP_DIR         = "backups";
    public static final int    BACKUP_MAX_KEEP    = 10;

    private AppConfig() { /* utility class */ }
}
