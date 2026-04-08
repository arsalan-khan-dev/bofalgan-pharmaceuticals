package com.bofalgan.pharmacy.db;

import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

/**
 * Creates all database tables if they don't exist.
 * Also seeds default admin user and default categories on first run.
 */
public class SchemaInitializer {

    private final DatabaseManager db;

    public SchemaInitializer(DatabaseManager db) {
        this.db = db;
    }

    public void initialize() {
        try (Connection conn = db.getConnection()) {
            createTables(conn);
            seedDefaultData(conn);
            System.out.println("[Schema] All tables initialized.");
        } catch (Exception e) {
            throw new DatabaseException("Schema initialization failed: " + e.getMessage(), e);
        }
    }

    private void createTables(Connection conn) throws Exception {
        try (Statement stmt = conn.createStatement()) {

            // medicine_categories
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS medicine_categories (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    name        VARCHAR(100) NOT NULL UNIQUE,
                    description TEXT,
                    is_active   TINYINT(1) DEFAULT 1,
                    created_at  TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // suppliers
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS suppliers (
                    id             INT AUTO_INCREMENT PRIMARY KEY,
                    name           VARCHAR(200) NOT NULL UNIQUE,
                    contact_person VARCHAR(150),
                    phone          VARCHAR(30),
                    email          VARCHAR(150),
                    address        TEXT,
                    city           VARCHAR(100),
                    state          VARCHAR(100),
                    gstin          VARCHAR(50),
                    payment_terms  VARCHAR(50),
                    is_active      TINYINT(1) DEFAULT 1,
                    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // users
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS users (
                    id                    INT AUTO_INCREMENT PRIMARY KEY,
                    username              VARCHAR(80) NOT NULL UNIQUE,
                    password_hash         VARCHAR(255) NOT NULL,
                    full_name             VARCHAR(150) NOT NULL,
                    role                  ENUM('ADMIN','STAFF') NOT NULL DEFAULT 'STAFF',
                    email                 VARCHAR(150),
                    phone                 VARCHAR(30),
                    is_active             TINYINT(1) DEFAULT 1,
                    last_login            DATETIME,
                    failed_login_attempts INT DEFAULT 0,
                    locked_until          DATETIME,
                    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    created_by_user_id    INT
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // medicines
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS medicines (
                    id                   INT AUTO_INCREMENT PRIMARY KEY,
                    name                 VARCHAR(200) NOT NULL,
                    generic_name         VARCHAR(200),
                    category             VARCHAR(100),
                    strength             VARCHAR(50),
                    unit                 VARCHAR(50),
                    batch_number         VARCHAR(100) NOT NULL,
                    quantity             INT NOT NULL DEFAULT 0,
                    reorder_level        INT NOT NULL DEFAULT 10,
                    purchase_price       DECIMAL(12,2) NOT NULL,
                    selling_price        DECIMAL(12,2) NOT NULL,
                    supplier_id          INT,
                    expiry_date          DATE NOT NULL,
                    storage_location     VARCHAR(100),
                    is_prescription_only TINYINT(1) DEFAULT 0,
                    is_deleted           TINYINT(1) DEFAULT 0,
                    created_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    updated_at           TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                    barcode              VARCHAR(100) UNIQUE,
                    FOREIGN KEY (supplier_id) REFERENCES suppliers(id) ON DELETE SET NULL
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // purchases
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchases (
                    id                INT AUTO_INCREMENT PRIMARY KEY,
                    supplier_id       INT NOT NULL,
                    purchase_date     DATE NOT NULL,
                    delivery_date     DATE,
                    total_amount      DECIMAL(14,2) NOT NULL,
                    tax_amount        DECIMAL(12,2) DEFAULT 0,
                    discount_amount   DECIMAL(12,2) DEFAULT 0,
                    notes             TEXT,
                    payment_status    ENUM('PAID','PARTIAL','PENDING') DEFAULT 'PENDING',
                    paid_amount       DECIMAL(14,2) DEFAULT 0,
                    created_by_user_id INT NOT NULL,
                    created_at        TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (supplier_id) REFERENCES suppliers(id),
                    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // purchase_items
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS purchase_items (
                    id                INT AUTO_INCREMENT PRIMARY KEY,
                    purchase_id       INT NOT NULL,
                    medicine_id       INT NOT NULL,
                    batch_number      VARCHAR(100) NOT NULL,
                    quantity_received INT NOT NULL,
                    quantity_accepted INT NOT NULL,
                    unit_price        DECIMAL(12,2) NOT NULL,
                    expiry_date       DATE NOT NULL,
                    notes             TEXT,
                    FOREIGN KEY (purchase_id) REFERENCES purchases(id) ON DELETE CASCADE,
                    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // invoices
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoices (
                    id                 INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_number     VARCHAR(30) NOT NULL UNIQUE,
                    customer_name      VARCHAR(150),
                    customer_phone     VARCHAR(30),
                    customer_email     VARCHAR(150),
                    subtotal           DECIMAL(14,2) NOT NULL,
                    discount_type      ENUM('PERCENTAGE','FIXED','NONE') DEFAULT 'NONE',
                    discount_value     DECIMAL(10,2) DEFAULT 0,
                    tax_amount         DECIMAL(12,2) DEFAULT 0,
                    total_amount       DECIMAL(14,2) NOT NULL,
                    paid_amount        DECIMAL(14,2) NOT NULL,
                    payment_method     ENUM('CASH','CARD','CREDIT','CHEQUE') DEFAULT 'CASH',
                    payment_status     ENUM('PAID','PARTIAL','DUE') DEFAULT 'PAID',
                    notes              TEXT,
                    created_by_user_id INT NOT NULL,
                    created_at         TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (created_by_user_id) REFERENCES users(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // invoice_items
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS invoice_items (
                    id          INT AUTO_INCREMENT PRIMARY KEY,
                    invoice_id  INT NOT NULL,
                    medicine_id INT NOT NULL,
                    batch_number VARCHAR(100),
                    quantity    INT NOT NULL,
                    unit_price  DECIMAL(12,2) NOT NULL,
                    line_total  DECIMAL(14,2) NOT NULL,
                    FOREIGN KEY (invoice_id) REFERENCES invoices(id) ON DELETE CASCADE,
                    FOREIGN KEY (medicine_id) REFERENCES medicines(id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // activity_log
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS activity_log (
                    id              INT AUTO_INCREMENT PRIMARY KEY,
                    user_id         INT NOT NULL,
                    username        VARCHAR(80),
                    action          VARCHAR(30) NOT NULL,
                    entity_type     VARCHAR(50),
                    entity_id       INT,
                    entity_name     VARCHAR(200),
                    old_value       TEXT,
                    new_value       TEXT,
                    changes_summary TEXT,
                    ip_address      VARCHAR(50),
                    timestamp       TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_al_user (user_id),
                    INDEX idx_al_time (timestamp)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // alerts
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS alerts (
                    id                    INT AUTO_INCREMENT PRIMARY KEY,
                    alert_type            VARCHAR(30) NOT NULL,
                    severity              ENUM('INFO','WARNING','CRITICAL') DEFAULT 'INFO',
                    entity_type           VARCHAR(50),
                    entity_id             INT,
                    entity_name           VARCHAR(200),
                    message               TEXT NOT NULL,
                    is_dismissed          TINYINT(1) DEFAULT 0,
                    dismissed_at          DATETIME,
                    dismissed_by_user_id  INT,
                    created_at            TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_alerts_type (alert_type),
                    INDEX idx_alerts_dismissed (is_dismissed)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // settings
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS settings (
                    `key`       VARCHAR(100) PRIMARY KEY,
                    value       TEXT,
                    data_type   VARCHAR(20) DEFAULT 'STRING',
                    description TEXT,
                    is_system   TINYINT(1) DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // customers
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS customers (
                    id              INT AUTO_INCREMENT PRIMARY KEY,
                    full_name       VARCHAR(150) NOT NULL,
                    phone           VARCHAR(30) UNIQUE,
                    email           VARCHAR(150),
                    address         TEXT,
                    date_of_birth   DATE,
                    created_at      TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    total_purchases INT DEFAULT 0,
                    total_spent     DECIMAL(14,2) DEFAULT 0
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);
        }
    }

    private void seedDefaultData(Connection conn) throws Exception {
        seedAdminUser(conn);
        seedCategories(conn);
        seedSettings(conn);
    }

    private void seedAdminUser(Connection conn) throws Exception {
        String check = "SELECT COUNT(*) FROM users WHERE username = 'admin'";
        try (PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        // BCrypt hash of "Admin@123"
        String hash = "$2a$12$2VxmPUEuMwTJHHjJ9r8pI.EZDQsTSBKJqFHKNEUGHHAvE4VfFPT9i";
        String insert = """
            INSERT INTO users (username, password_hash, full_name, role, is_active)
            VALUES ('admin', ?, 'System Administrator', 'ADMIN', 1)
        """;
        try (PreparedStatement ps = conn.prepareStatement(insert)) {
            ps.setString(1, hash);
            ps.executeUpdate();
            System.out.println("[Schema] Default admin user created. Login: admin / Admin@123");
        }
    }

    private void seedCategories(Connection conn) throws Exception {
        String check = "SELECT COUNT(*) FROM medicine_categories";
        try (PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String[] cats = {
            "Antibiotics", "Antifungals", "Antivirals", "Analgesics",
            "Anti-inflammatory", "Vitamins & Supplements", "Antacids",
            "Antidiabetics", "Antihypertensives", "Antihistamines",
            "Cough & Cold", "Dermatology", "Eye & Ear Drops",
            "Hormones", "Injections", "Syrups", "Vaccines", "Other"
        };
        String sql = "INSERT IGNORE INTO medicine_categories (name) VALUES (?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (String cat : cats) {
                ps.setString(1, cat);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    private void seedSettings(Connection conn) throws Exception {
        String check = "SELECT COUNT(*) FROM settings";
        try (PreparedStatement ps = conn.prepareStatement(check);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            if (rs.getInt(1) > 0) return;
        }
        String sql = "INSERT IGNORE INTO settings (`key`, value, data_type, description) VALUES (?,?,?,?)";
        Object[][] defaults = {
            {"pharmacy_name",      "Bofalgan Pharmaceuticals", "STRING",  "Pharmacy display name"},
            {"pharmacy_address",   "",                          "STRING",  "Pharmacy address"},
            {"pharmacy_phone",     "",                          "STRING",  "Pharmacy phone"},
            {"pharmacy_license",   "",                          "STRING",  "License number"},
            {"currency_symbol",    "$",                         "STRING",  "Currency symbol"},
            {"invoice_prefix",     "INV",                       "STRING",  "Invoice number prefix"},
            {"default_tax_rate",   "0",                         "DECIMAL", "Default tax rate %"},
            {"invoice_footer",     "Thank you for your purchase!", "STRING","Invoice footer text"},
            {"reorder_level",      "10",                        "INTEGER", "Default reorder level"},
            {"expiry_warning_days","30",                        "INTEGER", "Days for expiry warning"},
            {"expiry_critical_days","7",                        "INTEGER", "Days for critical expiry"},
            {"auto_logout_minutes","30",                        "INTEGER", "Session timeout (0=disabled)"},
            {"max_failed_logins",  "3",                         "INTEGER", "Max failed login attempts"},
            {"lock_duration_mins", "15",                        "INTEGER", "Account lock duration"},
            {"backup_enabled",     "true",                      "BOOLEAN", "Enable auto backup"},
            {"backup_frequency",   "DAILY",                     "STRING",  "Backup frequency"},
            {"backup_folder",      "backups",                   "STRING",  "Backup destination folder"},
        };
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (Object[] row : defaults) {
                ps.setString(1, (String) row[0]);
                ps.setString(2, (String) row[1]);
                ps.setString(3, (String) row[2]);
                ps.setString(4, (String) row[3]);
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }
}
