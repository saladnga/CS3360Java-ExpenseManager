import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHandler {
    private Connection connection;

    public void connect() throws SQLException {
        String url = "jdbc:sqlite:expenses.db";
        connection = DriverManager.getConnection(url);
        System.out.println("Connected to SQLite Database!");

        String sql = "CREATE TABLE IF NOT EXISTS expenses (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "date TEXT, " +
                "name TEXT, " +
                "amount REAL, " +
                "category TEXT, " +
                "description TEXT" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(sql);
            System.out.println("Expenses table ready.");
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }


    // CREATE
    public void saveExpense(Expense e) {
        String sql = "INSERT INTO expenses (date, name, amount, category, description) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, e.getDate());
            stmt.setString(2, e.getName());
            stmt.setDouble(3, e.getAmount());
            stmt.setString(4, e.getCategory());
            stmt.setString(5, e.getDescription());
            stmt.executeUpdate();
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    // READ
    public List<Expense> getAllExpenses() {
        List<Expense> list = new ArrayList<>();
        String sql = "SELECT * FROM expenses";
        try (Statement stmt = connection.createStatement();
                ResultSet rs = stmt.executeQuery(sql)) {
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
    public boolean updateExpense(Expense e) {
        String sql = "UPDATE expenses SET date=?, name=?, amount=?, category=?, description=? WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, e.getDate());
            stmt.setString(2, e.getName());
            stmt.setDouble(3, e.getAmount());
            stmt.setString(4, e.getCategory());
            stmt.setString(5, e.getDescription());
            stmt.setInt(6, e.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    // DELETE
    public boolean deleteExpense(Expense e) {
        String sql = "DELETE FROM expenses WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, e.getId());
            return stmt.executeUpdate() > 0;
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    // Retrieve by ID
    public Expense getExpenseById(int id) {
        String sql = "SELECT * FROM expenses WHERE id=?";
        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setInt(1, id);
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
