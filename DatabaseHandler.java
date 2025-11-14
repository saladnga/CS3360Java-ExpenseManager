import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private Connection connection;

    public void connect() throws SQLException {
        String url = "jdbc:sqlite:expenses.db";
        connection = DriverManager.getConnection(url);
        System.out.println("Connected to SQLite Database!");

        String userSql = "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "username TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL" +
                ");";

        String expenseSql = "CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT, " +
                "name TEXT, " +
                "amount REAL, " +
                "category TEXT, " +
                "description TEXT, " +
                "user_id INTEGER, " +
                "FOREIGN KEY(user_id) REFERENCES users(id)" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.executeUpdate(userSql);
            stmt.execute(expenseSql);
            System.out.println("Expenses and Users table ready.");
        } catch (SQLException ex) {
            ex.printStackTrace();
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
            ex.printStackTrace();
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
            if (res.next()) {
                return res.getInt("id");
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return -1;
    }


    // CREATE
    public void saveExpense(Expense e, int userId) {
        String sql = "INSERT INTO expenses (date, name, amount, category, description, user_id) VALUES (?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, e.getDate());
            stmt.setString(2, e.getName());
            stmt.setDouble(3, e.getAmount());
            stmt.setString(4, e.getCategory());
            stmt.setString(5, e.getDescription());
            stmt.setInt(6, userId);
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
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
                Expense e = new Expense(
                        rs.getInt("id"),
                        rs.getString("date"),
                        rs.getString("name"),
                        rs.getDouble("amount"),
                        rs.getString("category"),
                        rs.getString("description"));
                list.add(e);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return list;
    }

    // UPDATE
    public boolean updateExpense(Expense e, int userId) {
        String sql = "UPDATE expenses SET date=?, name=?, amount=?, category=?, description=? WHERE id=? AND user_id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, e.getDate());
            stmt.setString(2, e.getName());
            stmt.setDouble(3, e.getAmount());
            stmt.setString(4, e.getCategory());
            stmt.setString(5, e.getDescription());
            stmt.setInt(6, e.getId());
            stmt.setInt(7, userId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
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
            ex.printStackTrace();
        }
        return false;
    }

    // Retrieve by ID
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
                        rs.getString("category"),
                        rs.getString("description"));
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
