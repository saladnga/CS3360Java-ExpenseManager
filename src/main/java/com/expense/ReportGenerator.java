package com.expense;

import java.sql.*;
import java.util.*;

/**
 * The ReportGenerator class provides summarized reports
 * of expenses by month, category, and overall totals.
 */
public class ReportGenerator {

    private final Connection connection; // FIX: mark as final

    public ReportGenerator(Connection connection) {
        this.connection = connection;
    }

    /**
     * Generates a summary of total expenses per month.
     *
     * @return A map where the key is the month (e.g. "2025-11")
     *         and the value is the total amount spent.
     */
    public Map<String, Double> generateMonthlySummary() {

        Map<String, Double> summary = new LinkedHashMap<>();

        // NOTE: "substr" is correct SQL function for SQLite
        String sql = """
            SELECT substr(date, 1, 7) AS month, SUM(amount) AS total
            FROM expenses
            GROUP BY month
            ORDER BY month ASC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                summary.put(rs.getString("month"), rs.getDouble("total"));
            }

        } catch (SQLException e) {
            System.err.println("SQL Error (monthly summary): " + e.getMessage());
        }

        return summary;
    }

    /**
     * Generates a summary of total expenses per category.
     *
     * @return A map where the key is the category name
     *         and the value is the total amount spent in that category.
     */
    public Map<String, Double> generateCategorySummary() {

        Map<String, Double> summary = new LinkedHashMap<>();

        String sql = """
            SELECT category, SUM(amount) AS total
            FROM expenses
            GROUP BY category
            ORDER BY total DESC
        """;

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                summary.put(rs.getString("category"), rs.getDouble("total"));
            }

        } catch (SQLException e) {
            System.err.println("SQL Error (category summary): " + e.getMessage());
        }

        return summary;
    }

    /**
     * Calculates the overall total amount of all expenses.
     *
     * @return The total sum of all expenses.
     */
    public double generateTotalSummary() {

        String sql = "SELECT SUM(amount) AS total FROM expenses";

        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            if (rs.next()) {
                return rs.getDouble("total");
            }

        } catch (SQLException e) {
            System.err.println("SQL Error (total summary): " + e.getMessage());
        }

        return 0.0;
    }
}
