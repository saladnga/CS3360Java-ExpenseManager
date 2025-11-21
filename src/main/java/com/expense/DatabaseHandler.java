package com.expense;

import java.sql.*;
import java.util.*;

public class DatabaseHandler {

    private Connection connection;

    // ================= CATEGORY NORMALIZATION ==================
    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();

    static {
        CATEGORY_MAP.put("food", "Food & Drinks");
        CATEGORY_MAP.put("foods", "Food & Drinks");
        CATEGORY_MAP.put("drink", "Food & Drinks");
        CATEGORY_MAP.put("drinks", "Food & Drinks");
        CATEGORY_MAP.put("meal", "Food & Drinks");
        CATEGORY_MAP.put("food & drinks", "Food & Drinks");
        CATEGORY_MAP.put("lunch", "Food & Drinks");
        CATEGORY_MAP.put("dinner", "Food & Drinks");

        CATEGORY_MAP.put("utilities", "Utilities");

        CATEGORY_MAP.put("care", "Personal Care");
        CATEGORY_MAP.put("personal care", "Personal Care");

        CATEGORY_MAP.put("entertainment", "Entertainment");
        CATEGORY_MAP.put("movie", "Entertainment");
        CATEGORY_MAP.put("cinema", "Entertainment");

        CATEGORY_MAP.put("education", "Education");
        CATEGORY_MAP.put("school", "Education");
        CATEGORY_MAP.put("course", "Education");

        CATEGORY_MAP.put("health", "Health");
        CATEGORY_MAP.put("medical", "Health");

        CATEGORY_MAP.put("transport", "Transportation");
        CATEGORY_MAP.put("transportation", "Transportation");
        CATEGORY_MAP.put("taxi", "Transportation");
        CATEGORY_MAP.put("grab", "Transportation");

        CATEGORY_MAP.put("electronic", "Electronics");
        CATEGORY_MAP.put("electronics", "Electronics");
        CATEGORY_MAP.put("device", "Electronics");

        CATEGORY_MAP.put("sport", "Sports");
        CATEGORY_MAP.put("sports", "Sports");
    }

    private String normalizeCategory(String input) {
        if (input == null || input.trim().isEmpty())
            return "Other";

        input = input.toLowerCase().trim();

        if (CATEGORY_MAP.containsKey(input))
            return CATEGORY_MAP.get(input);

        // fuzzy match
        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String key : CATEGORY_MAP.keySet()) {
            int dist = levenshteinDistance(input, key);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestMatch = key;
            }
        }

        if (bestDistance <= 2)
            return CATEGORY_MAP.get(bestMatch);

        return "Other";
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;

        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                if (a.charAt(i - 1) == b.charAt(j - 1))
                    dp[i][j] = dp[i - 1][j - 1];
                else
                    dp[i][j] = 1 + Math.min(dp[i - 1][j - 1], Math.min(dp[i - 1][j], dp[i][j - 1]));
            }
        }
        return dp[a.length()][b.length()];
    }

    // ===========================================================

    public void connect() throws SQLException {
        String url = "jdbc:sqlite:expenses.db";
        connection = DriverManager.getConnection(url);

        System.out.println("Connected to SQLite Database!");

        String userSql = """
                CREATE TABLE IF NOT EXISTS users (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    username TEXT UNIQUE NOT NULL,
                    password TEXT NOT NULL
                );
                """;

        String expenseSql = """
                CREATE TABLE IF NOT EXISTS expenses (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    date TEXT,
                    name TEXT,
                    amount REAL,
                    category TEXT,
                    description TEXT,
                    user_id INTEGER,
                    FOREIGN KEY(user_id) REFERENCES users(id)
                );
                """;

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(userSql);
            stmt.execute(expenseSql);
            System.out.println("Expenses and Users table ready.");
        }
    }

    // REGISTER
    public boolean register(User user) {
        String sql = "INSERT INTO users (username, password) VALUES (?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            stmt.executeUpdate();
            return true;
        } catch (SQLException ex) {
            System.err.println("Register failed: " + ex.getMessage());
            return false;
        }
    }

    // LOGIN
    public int login(User user) {
        String sql = "SELECT * FROM users WHERE username = ? AND password = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, user.getUsername());
            stmt.setString(2, user.getPassword());
            ResultSet res = stmt.executeQuery();
            if (res.next()) return res.getInt("id");
        } catch (SQLException ex) {
            System.err.println("Login failed: " + ex.getMessage());
        }
        return -1;
    }

    // CREATE
    public void saveExpense(Expense e, int userId) {
        String sql = "INSERT INTO expenses (date, name, amount, category, description, user_id) VALUES (?, ?, ?, ?, ?, ?)";

        String normalizedCategory = normalizeCategory(e.getCategory());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, e.getDate());
            stmt.setString(2, e.getName());
            stmt.setDouble(3, e.getAmount());
            stmt.setString(4, normalizedCategory);
            stmt.setString(5, e.getDescription());
            stmt.setInt(6, userId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            System.err.println("Save expense failed: " + ex.getMessage());
        }
    }

    // READ
    public List<Expense> getAllExpenses(int userId) {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses WHERE user_id = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            ResultSet rs = stmt.executeQuery();

            while (rs.next()) {
                list.add(new Expense(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        normalizeCategory(rs.getString("category")),
                        rs.getString("description")
                ));
            }

        } catch (SQLException ex) {
            System.err.println("Load expenses failed: " + ex.getMessage());
        }
        return list;
    }

    // UPDATE
    public boolean updateExpense(Expense e, int userId) {
        String sql = "UPDATE expenses SET date=?, name=?, amount=?, category=?, description=? WHERE id=? AND user_id=?";

        String normalizedCategory = normalizeCategory(e.getCategory());

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, e.getDate());
            stmt.setString(2, e.getName());
            stmt.setDouble(3, e.getAmount());
            stmt.setString(4, normalizedCategory);
            stmt.setString(5, e.getDescription());
            stmt.setInt(6, e.getId());
            stmt.setInt(7, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Update failed: " + ex.getMessage());
        }
        return false;
    }

    // DELETE
    public boolean deleteExpense(Expense e, int userId) {
        String sql = "DELETE FROM expenses WHERE id=? AND user_id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, e.getId());
            stmt.setInt(2, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            System.err.println("Delete failed: " + ex.getMessage());
        }
        return false;
    }

    // GET BY ID
    public Expense getExpenseById(int id, int userId) {
        String sql = "SELECT * FROM expenses WHERE id=? AND user_id=?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.setInt(2, userId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                return new Expense(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        normalizeCategory(rs.getString("category")),
                        rs.getString("description")
                );
            }

        } catch (SQLException ex) {
            System.err.println("Get expense failed: " + ex.getMessage());
        }
        return null;
    }

    public void deleteAllExpenses(int userId) {
        String sql = "DELETE FROM expenses WHERE user_id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, userId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("deleteAllExpenses failed: " + e.getMessage());
        }
    }

    public void resetAutoIncrement() {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("DELETE FROM sqlite_sequence WHERE name='expenses'");
        } catch (SQLException e) {
            System.err.println("resetAutoIncrement failed: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        return connection;
    }
}
