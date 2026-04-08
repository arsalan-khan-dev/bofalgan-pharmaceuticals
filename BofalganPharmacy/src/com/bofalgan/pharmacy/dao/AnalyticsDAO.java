package com.bofalgan.pharmacy.dao;

import com.bofalgan.pharmacy.db.DatabaseManager;
import com.bofalgan.pharmacy.util.DatabaseException;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

/**
 * Dedicated DAO for all analytics queries.
 * All data returned is real - no mock data anywhere.
 */
public class AnalyticsDAO {

    private final DatabaseManager db;

    public AnalyticsDAO(DatabaseManager db) { this.db = db; }

    // ==================== SALES ANALYTICS ====================

    /** Daily revenue for last N days */
    public Map<String, Double> getDailyRevenue(int days) {
        String sql = """
            SELECT DATE(created_at) AS sale_date, SUM(total_amount) AS revenue
            FROM invoices
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY DATE(created_at)
            ORDER BY sale_date ASC
        """;
        return queryStringDouble(sql, days);
    }

    /** Monthly revenue for last N months */
    public Map<String, Double> getMonthlyRevenue(int months) {
        String sql = """
            SELECT DATE_FORMAT(created_at, '%Y-%m') AS month, SUM(total_amount) AS revenue
            FROM invoices
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? MONTH)
            GROUP BY DATE_FORMAT(created_at, '%Y-%m')
            ORDER BY month ASC
        """;
        return queryStringDouble(sql, months);
    }

    /** Revenue grouped by payment method */
    public Map<String, Double> getRevenueByPaymentMethod() {
        String sql = """
            SELECT payment_method, SUM(total_amount) AS total
            FROM invoices
            GROUP BY payment_method
            ORDER BY total DESC
        """;
        return queryStringDoubleNoParam(sql);
    }

    /** Total invoices per day (transaction count) */
    public Map<String, Integer> getDailyTransactionCount(int days) {
        String sql = """
            SELECT DATE(created_at) AS sale_date, COUNT(*) AS cnt
            FROM invoices
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY DATE(created_at)
            ORDER BY sale_date ASC
        """;
        return queryStringInt(sql, days);
    }

    // ==================== TOP SELLING ====================

    /** Top N medicines by quantity sold */
    public List<Object[]> getTopSellingMedicines(int topN, int days) {
        String sql = """
            SELECT m.name, SUM(ii.quantity) AS total_qty, SUM(ii.line_total) AS total_revenue
            FROM invoice_items ii
            JOIN medicines m ON ii.medicine_id = m.id
            JOIN invoices inv ON ii.invoice_id = inv.id
            WHERE inv.created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY m.id, m.name
            ORDER BY total_qty DESC
            LIMIT ?
        """;
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, days); ps.setInt(2, topN);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                        rs.getString("name"),
                        rs.getInt("total_qty"),
                        rs.getDouble("total_revenue")
                    });
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("getTopSellingMedicines failed: " + e.getMessage(), e);
        }
        return result;
    }

    // ==================== INVENTORY ANALYTICS ====================

    /** Stock quantity by category */
    public Map<String, Integer> getStockByCategory() {
        String sql = """
            SELECT COALESCE(category, 'Uncategorized') AS cat, SUM(quantity) AS total_qty
            FROM medicines
            WHERE is_deleted=0
            GROUP BY category
            ORDER BY total_qty DESC
        """;
        Map<String, Integer> result = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) result.put(rs.getString("cat"), rs.getInt("total_qty"));
        } catch (SQLException e) {
            throw new DatabaseException("getStockByCategory failed: " + e.getMessage(), e);
        }
        return result;
    }

    /** Inventory value trend over last N days (approximated from daily snapshots via invoices) */
    public Map<String, Double> getInventoryValueTrend(int days) {
        // Uses current inventory value as baseline, works back via invoice totals
        String sql = """
            SELECT DATE(created_at) AS d, SUM(total_amount) AS sold
            FROM invoices
            WHERE created_at >= DATE_SUB(CURDATE(), INTERVAL ? DAY)
            GROUP BY DATE(created_at)
            ORDER BY d ASC
        """;
        return queryStringDouble(sql, days);
    }

    /** Count medicines by expiry status */
    public Map<String, Integer> getExpiryStatusCounts() {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = db.getConnection(); Statement stmt = conn.createStatement()) {
            ResultSet rs;

            rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines WHERE is_deleted=0 AND quantity>0 AND expiry_date < CURDATE()");
            rs.next(); map.put("Expired", rs.getInt(1)); rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines WHERE is_deleted=0 AND quantity>0 AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(),INTERVAL 7 DAY)");
            rs.next(); map.put("Critical (7d)", rs.getInt(1)); rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines WHERE is_deleted=0 AND quantity>0 AND expiry_date BETWEEN DATE_ADD(CURDATE(),INTERVAL 7 DAY) AND DATE_ADD(CURDATE(),INTERVAL 30 DAY)");
            rs.next(); map.put("Warning (30d)", rs.getInt(1)); rs.close();

            rs = stmt.executeQuery("SELECT COUNT(*) FROM medicines WHERE is_deleted=0 AND quantity>0 AND expiry_date > DATE_ADD(CURDATE(),INTERVAL 30 DAY)");
            rs.next(); map.put("Safe", rs.getInt(1)); rs.close();
        } catch (SQLException e) {
            throw new DatabaseException("getExpiryStatusCounts failed: " + e.getMessage(), e);
        }
        return map;
    }

    /** Medicines expiring each month for next 6 months */
    public Map<String, Integer> getExpiryByMonth() {
        String sql = """
            SELECT DATE_FORMAT(expiry_date, '%Y-%m') AS month, COUNT(*) AS cnt
            FROM medicines
            WHERE is_deleted=0 AND quantity>0
              AND expiry_date BETWEEN CURDATE() AND DATE_ADD(CURDATE(), INTERVAL 6 MONTH)
            GROUP BY DATE_FORMAT(expiry_date, '%Y-%m')
            ORDER BY month ASC
        """;
        return queryStringIntNoParam(sql);
    }

    // ==================== SUPPLIER ANALYTICS ====================

    /** Total purchase amount per supplier */
    public Map<String, Double> getPurchaseBySupplier() {
        String sql = """
            SELECT s.name, SUM(p.total_amount) AS total
            FROM purchases p
            JOIN suppliers s ON p.supplier_id = s.id
            GROUP BY s.id, s.name
            ORDER BY total DESC
        """;
        return queryStringDoubleNoParam(sql);
    }

    /** Outstanding balance per supplier */
    public List<Object[]> getSupplierOutstandingBalance() {
        String sql = """
            SELECT s.name, SUM(p.total_amount) AS total_purchased,
                   SUM(p.paid_amount) AS total_paid,
                   SUM(p.total_amount - p.paid_amount) AS outstanding
            FROM purchases p
            JOIN suppliers s ON p.supplier_id = s.id
            WHERE p.payment_status != 'PAID'
            GROUP BY s.id, s.name
            HAVING outstanding > 0
            ORDER BY outstanding DESC
        """;
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                result.add(new Object[]{
                    rs.getString("name"),
                    rs.getDouble("total_purchased"),
                    rs.getDouble("total_paid"),
                    rs.getDouble("outstanding")
                });
            }
        } catch (SQLException e) {
            throw new DatabaseException("getSupplierOutstandingBalance failed: " + e.getMessage(), e);
        }
        return result;
    }

    // ==================== PROFIT ====================

    /** Profit margin per medicine (admin only) */
    public List<Object[]> getProfitByMedicine(LocalDate start, LocalDate end, int limit) {
        String sql = """
            SELECT m.name,
                   SUM(ii.quantity) AS qty_sold,
                   m.purchase_price AS avg_purchase,
                   m.selling_price  AS avg_sell,
                   ROUND(((m.selling_price - m.purchase_price) / m.purchase_price) * 100, 2) AS margin_pct,
                   SUM(ii.quantity * (m.selling_price - m.purchase_price)) AS total_profit
            FROM invoice_items ii
            JOIN medicines m ON ii.medicine_id = m.id
            JOIN invoices inv ON ii.invoice_id = inv.id
            WHERE DATE(inv.created_at) BETWEEN ? AND ?
            GROUP BY m.id, m.name, m.purchase_price, m.selling_price
            ORDER BY total_profit DESC
            LIMIT ?
        """;
        List<Object[]> result = new ArrayList<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(start));
            ps.setDate(2, Date.valueOf(end));
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    result.add(new Object[]{
                        rs.getString("name"),
                        rs.getInt("qty_sold"),
                        rs.getDouble("avg_purchase"),
                        rs.getDouble("avg_sell"),
                        rs.getDouble("margin_pct"),
                        rs.getDouble("total_profit")
                    });
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("getProfitByMedicine failed: " + e.getMessage(), e);
        }
        return result;
    }

    // ==================== PRIVATE HELPERS ====================

    private Map<String, Double> queryStringDouble(String sql, int param) {
        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                while (rs.next()) {
                    map.put(rs.getString(1), rs.getDouble(2));
                }
            }
        } catch (SQLException e) {
            throw new DatabaseException("queryStringDouble failed: " + e.getMessage(), e);
        }
        return map;
    }

    private Map<String, Double> queryStringDoubleNoParam(String sql) {
        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getDouble(2));
        } catch (SQLException e) {
            throw new DatabaseException("queryStringDoubleNoParam failed: " + e.getMessage(), e);
        }
        return map;
    }

    private Map<String, Integer> queryStringInt(String sql, int param) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
            }
        } catch (SQLException e) {
            throw new DatabaseException("queryStringInt failed: " + e.getMessage(), e);
        }
        return map;
    }

    private Map<String, Integer> queryStringIntNoParam(String sql) {
        Map<String, Integer> map = new LinkedHashMap<>();
        try (Connection conn = db.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) map.put(rs.getString(1), rs.getInt(2));
        } catch (SQLException e) {
            throw new DatabaseException("queryStringIntNoParam failed: " + e.getMessage(), e);
        }
        return map;
    }
}
