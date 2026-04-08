package com.bofalgan.pharmacy.db;

import com.bofalgan.pharmacy.config.AppConfig;
import com.bofalgan.pharmacy.util.DatabaseException;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Singleton MySQL connection pool manager using HikariCP.
 * All DAOs obtain connections from this pool.
 */
public class DatabaseManager {

    private static DatabaseManager instance;
    private HikariDataSource dataSource;
    private boolean connected = false;

    private DatabaseManager() {}

    public static synchronized DatabaseManager getInstance() {
        if (instance == null) {
            instance = new DatabaseManager();
        }
        return instance;
    }

    /**
     * Initializes the HikariCP pool. Call once on application startup.
     */
    public void initialize() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(AppConfig.DB_URL);
            config.setUsername(AppConfig.DB_USER);
            config.setPassword(AppConfig.DB_PASSWORD);
            config.setDriverClassName("com.mysql.cj.jdbc.Driver");

            config.setMaximumPoolSize(AppConfig.DB_POOL_SIZE);
            config.setMinimumIdle(AppConfig.DB_POOL_MIN);
            config.setConnectionTimeout(AppConfig.DB_POOL_TIMEOUT);
            config.setIdleTimeout(600_000);
            config.setMaxLifetime(1_800_000);
            config.setConnectionTestQuery("SELECT 1");
            config.setPoolName("BofalganPool");

            // Performance tuning
            config.addDataSourceProperty("cachePrepStmts",          "true");
            config.addDataSourceProperty("prepStmtCacheSize",       "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit",   "2048");
            config.addDataSourceProperty("useServerPrepStmts",      "true");
            config.addDataSourceProperty("useLocalSessionState",    "true");
            config.addDataSourceProperty("rewriteBatchedStatements", "true");
            config.addDataSourceProperty("cacheResultSetMetadata",  "true");
            config.addDataSourceProperty("cacheServerConfiguration","true");
            config.addDataSourceProperty("elideSetAutoCommits",     "true");
            config.addDataSourceProperty("maintainTimeStats",       "false");

            dataSource = new HikariDataSource(config);
            connected  = true;
            System.out.println("[DB] MySQL pool initialized successfully.");
        } catch (Exception e) {
            connected = false;
            throw new DatabaseException("Failed to initialize database connection: " + e.getMessage(), e);
        }
    }

    /**
     * Returns a connection from the pool.
     * Caller is responsible for closing it (use try-with-resources).
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new DatabaseException("Database is not initialized or has been shut down.");
        }
        return dataSource.getConnection();
    }

    /**
     * Tests whether the database is reachable.
     */
    public boolean isConnected() {
        if (!connected || dataSource == null) return false;
        try (Connection conn = dataSource.getConnection()) {
            return conn.isValid(2);
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Gracefully shuts down the pool. Call on application exit.
     */
    public void shutdown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            connected = false;
            System.out.println("[DB] Connection pool shut down.");
        }
    }

    /**
     * Convenience: execute a runnable in a transaction.
     * Rolls back on any exception.
     */
    public void withTransaction(TransactionCallback callback) {
        try (Connection conn = getConnection()) {
            conn.setAutoCommit(false);
            try {
                callback.execute(conn);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw new DatabaseException("Transaction rolled back: " + e.getMessage(), e);
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new DatabaseException("Database error: " + e.getMessage(), e);
        }
    }

    @FunctionalInterface
    public interface TransactionCallback {
        void execute(Connection conn) throws Exception;
    }
}
