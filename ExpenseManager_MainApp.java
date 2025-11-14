
// Import SQL
import java.sql.SQLException;
import java.util.List;

// Import JavaFX
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;

// javac --module-path /Users/UserN/Desktop/TROY/CS-3360/project/lib --add-modules javafx.controls ExpenseManager_MainApp.java
// java --module-path /Users/UserN/Desktop/TROY/CS-3360/project/lib --add-modules javafx.controls -cp /Users/UserN/Desktop/TROY/CS-3360/project/lib/sqlite-jdbcjava --module-path /Users/UserN/Desktop/TROY/CS-3360/project/lib --add-modules javafx.controls -cp /Users/UserN/Desktop/TROY/CS-3360/project/lib/sqlite-jdbc-3.42.0.0.jar:. ExpenseManager_MainApp-3.42.0.0.jar:. ExpenseManager_MainApp

public class ExpenseManager_MainApp extends Application {

    private DatabaseHandler dbHandler;
    private TableView<Expense> tableView;
    private Integer currentUserId = -1;
    private Button logoutButton;

    @Override
    public void start(Stage primaryStage) {
        dbHandler = new DatabaseHandler();
        CSVHandler csvHandler = new CSVHandler();
        logoutButton = new Button("Log out");
        logoutButton.setPrefWidth(150);
        logoutButton.setOnAction(e -> logout());

        try {
            dbHandler.connect();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        boolean loggedIn = showLogin();
        if (!loggedIn) {
            System.out.println("User did not login. Exiting.");
            return;
        }

        BorderPane mainLayout = new BorderPane();

        // Left Sidebar
        VBox sidebar = new VBox(15);
        sidebar.setStyle("-fx-padding: 20; -fx-background-color: #2c3e58");
        sidebar.setPrefWidth(180);

        Label title = new Label("Menu");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold");

        // Buttons and add to sidebar
        Button createButton = new Button("Create Expense");
        Button readButton = new Button("View All Expenses");
        Button updateButton = new Button("Update Expense");
        Button deleteButton = new Button("Delete Expense");
        Button exportButton = new Button("Export Expense as CSV");
        Button importButton = new Button("Import CSV");
        createButton.setPrefWidth(150);
        readButton.setPrefWidth(150);
        deleteButton.setPrefWidth(150);
        updateButton.setPrefWidth(150);
        exportButton.setPrefWidth(150);
        importButton.setPrefWidth(150);
        sidebar.getChildren().addAll(title, new Separator(), createButton, readButton, deleteButton, updateButton, exportButton, importButton, logoutButton);

        // Right Sidebar (Charts)
        ChartService chartService = new ChartService();

        VBox rightSidebar = new VBox(15);
        rightSidebar.setPadding(new Insets(15));
        rightSidebar.setStyle("-fx-padding: 20; -fx-background-color: #f4f4f4");
        Label chartTitle = new Label("Visualization");
        chartTitle.setStyle("-fx-text-fill: #2c3e58; -fx-font-size: 16px; -fx-font-weight: bold");

        PieChart pieChart = new PieChart();
        pieChart.setLabelsVisible(true);
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");
        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Amount");

        StackedBarChart<String, Number> stackedBarChart = new StackedBarChart<>(xAxis, yAxis);
        rightSidebar.getChildren().addAll(chartTitle, pieChart, stackedBarChart);

        // Main Content: List all expenses
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));

        Label contentTitle = new Label("All Expenses");
        contentTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        tableView = new TableView<>();

        TableColumn<Expense, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));
        idCol.setPrefWidth(50);

        TableColumn<Expense, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameCol.setPrefWidth(150);

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));
        dateCol.setPrefWidth(100);

        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("amount"));
        amountCol.setPrefWidth(100);

        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));
        categoryCol.setPrefWidth(120);

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));
        descCol.setPrefWidth(200);

        tableView.getColumns().addAll(java.util.Arrays.<TableColumn<Expense, ?>>asList(
                idCol, nameCol, dateCol, amountCol, categoryCol, descCol));
        loadData();

        VBox.setVgrow(tableView, Priority.ALWAYS);
        content.getChildren().addAll(contentTitle, tableView);

        createButton.setOnAction(_ -> {
            createExpense();
            loadData();
            updateChart(rightSidebar, chartService);
        });

        readButton.setOnAction(_ -> {
            loadData();
            updateChart(rightSidebar, chartService);
        });

        updateButton.setOnAction(_ -> {
            updateExpense();
            loadData();
            updateChart(rightSidebar, chartService);
        });

        deleteButton.setOnAction(_ -> {
            deleteExpense();
            loadData();
            updateChart(rightSidebar, chartService);
        });

        exportButton.setOnAction(_ -> {
            try {
                List<Expense> expenses = dbHandler.getAllExpenses(currentUserId);
                System.out.println("Exporting " + expenses.size() + " expenses");
                for (Expense e : expenses) {
                    System.out.println(e.getId() + " - " + e.getName());
                }
                csvHandler.writeCSV("expenses_export.csv", expenses);
                loadData();
                updateChart(rightSidebar, chartService);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Exported file");
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to export file");
                alert.showAndWait();
            }
        });

        importButton.setOnAction(_ -> {
            try {
                List<Expense> imported = csvHandler.readCSV("/Users/UserN/Desktop/TROY/CS-3360/project/expenses_export.csv");
                for (Expense expense : imported) {
                    dbHandler.saveExpense(expense, currentUserId);
                }
                loadData();
                updateChart(rightSidebar, chartService);
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Imported file");
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to import file");
                alert.showAndWait();
            }
        });

        updateChart(rightSidebar, chartService);

        // Main Layout
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);
        mainLayout.setRight(rightSidebar);

        // Create Scene
        Scene scene = new Scene(mainLayout, 900, 600);
        primaryStage.setTitle("Expense Manager");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    // Create Expense Modal
    private void createExpense() {
        Stage formStage = new Stage();
        formStage.setTitle("Create Expense");

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label dateLabel = new Label("Date (YYYY-MM-DD):");
        TextField dateField = new TextField();

        Label amountLabel = new Label("Amount:");
        TextField amountField = new TextField();

        Label categoryLabel = new Label("Category:");
        TextField categoryField = new TextField();

        Label descriptionLabel = new Label("Description");
        TextField descriptionField = new TextField();

        Button saveButton = new Button("Save");

        saveButton.setOnAction(_ -> {
            try {
                double amount = Double.parseDouble(amountField.getText());
                Expense newExpense = new Expense(null, dateField.getText(), nameField.getText(), amount,
                        categoryField.getText(), descriptionField.getText());

                dbHandler.saveExpense(newExpense, currentUserId);
                formStage.close();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Amount must be a number");
                alert.showAndWait();
            }
        });

        VBox layout = new VBox(15);
        layout.setPadding(new javafx.geometry.Insets(15));
        layout.getChildren().addAll(
                nameLabel, nameField,
                dateLabel, dateField,
                amountLabel, amountField,
                categoryLabel, categoryField,
                descriptionLabel, descriptionField,
                saveButton);

        Scene scene = new Scene(layout, 400, 400);
        formStage.setScene(scene);
        formStage.show();
    }

    // Update Expense Modal
    private boolean updateExpense() {
        Stage formStage = new Stage();
        formStage.setTitle("Update Expense");

        Label idLabel = new Label("Expense ID:");
        TextField idField = new TextField();

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label dateLabel = new Label("Date (YYYY-MM-DD):");
        TextField dateField = new TextField();

        Label amountLabel = new Label("Amount:");
        TextField amountField = new TextField();

        Label categoryLabel = new Label("Category:");
        TextField categoryField = new TextField();

        Label descriptionLabel = new Label("Description");
        TextField descriptionField = new TextField();

        Button fetchButton = new Button("Choose Expense");
        Button saveButton = new Button("Save");

        final boolean[] result = { false };

        // Get Expense
        fetchButton.setOnAction(_ -> {
            try {
                int id = Integer.parseInt(idField.getText());
                Expense expenseToUpdate = dbHandler.getExpenseById(id, currentUserId);

                if (expenseToUpdate != null) {
                    nameField.setText(expenseToUpdate.getName());
                    dateField.setText(expenseToUpdate.getDate());
                    amountField.setText(String.valueOf(expenseToUpdate.getAmount()));
                    categoryField.setText(expenseToUpdate.getCategory());
                    descriptionField.setText(expenseToUpdate.getDescription());
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Expense with ID " + id + " not found.");
                    alert.showAndWait();
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid ID. Please enter a valid number.");
                alert.showAndWait();
            }
        });

        // Save Expense
        saveButton.setOnAction(_ -> {
            try {
                int id = Integer.parseInt(idField.getText());
                double amount = Double.parseDouble(amountField.getText());
                Expense expenseToUpdate = dbHandler.getExpenseById(id, currentUserId);

                if (expenseToUpdate != null) {
                    expenseToUpdate.setName(nameField.getText());
                    expenseToUpdate.setDate(dateField.getText());
                    expenseToUpdate.setAmount(amount);
                    expenseToUpdate.setCategory(categoryField.getText());
                    expenseToUpdate.setDescription(descriptionField.getText());

                    boolean updated = dbHandler.updateExpense(expenseToUpdate, currentUserId);
                    if (updated) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Expense updated successfully");
                        alert.showAndWait();
                        result[0] = true;
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to update expense");
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Expense with ID " + id + " not found.");
                    alert.showAndWait();
                }
                formStage.close();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input: ");
                alert.showAndWait();
            }
        });

        VBox layout = new VBox(15);
        layout.setPadding(new javafx.geometry.Insets(15));
        layout.getChildren().addAll(
                idLabel, idField,
                fetchButton,
                new Separator(),
                nameLabel, nameField,
                dateLabel, dateField,
                amountLabel, amountField,
                categoryLabel, categoryField,
                descriptionLabel, descriptionField,
                saveButton);

        Scene scene = new Scene(layout, 450, 450);
        formStage.setScene(scene);
        formStage.showAndWait();
        return result[0];
    }

    // Delete Expense Modal
    private boolean deleteExpense() {
        Stage formStage = new Stage();
        formStage.setTitle("Delete Expense");

        Label idLabel = new Label("Expense ID:");
        TextField idField = new TextField();

        Label nameLabel = new Label("Name:");
        TextField nameField = new TextField();

        Label dateLabel = new Label("Date (YYYY-MM-DD):");
        TextField dateField = new TextField();

        Label amountLabel = new Label("Amount:");
        TextField amountField = new TextField();

        Label categoryLabel = new Label("Category:");
        TextField categoryField = new TextField();

        Label descriptionLabel = new Label("Description");
        TextField descriptionField = new TextField();

        Button fetchButton = new Button("Choose Expense");
        Button deleteButton = new Button("Delete");

        final boolean[] result = { false };

        fetchButton.setOnAction(_ -> {
            try {
                int id = Integer.parseInt(idField.getText());
                Expense expenseToUpdate = dbHandler.getExpenseById(id, currentUserId);

                if (expenseToUpdate != null) {
                    nameField.setText(expenseToUpdate.getName());
                    dateField.setText(expenseToUpdate.getDate());
                    amountField.setText(String.valueOf(expenseToUpdate.getAmount()));
                    categoryField.setText(expenseToUpdate.getCategory());
                    descriptionField.setText(expenseToUpdate.getDescription());
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Expense with ID " + id + " not found.");
                    alert.showAndWait();
                }
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid ID. Please enter a valid number.");
                alert.showAndWait();
            }
        });

        deleteButton.setOnAction(_ -> {
            try {
                int id = Integer.parseInt(idField.getText());
                Expense expenseToDelete = dbHandler.getExpenseById(id, currentUserId);

                if (expenseToDelete != null) {

                    boolean deleted = dbHandler.deleteExpense(expenseToDelete, currentUserId);
                    if (deleted) {
                        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Expense deleted successfully");
                        alert.showAndWait();
                        result[0] = true;
                    } else {
                        Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to delete expense");
                        alert.showAndWait();
                    }
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR, "Expense with ID " + id + " not found.");
                    alert.showAndWait();
                }
                formStage.close();
            } catch (NumberFormatException ex) {
                Alert alert = new Alert(Alert.AlertType.ERROR, "Invalid input: ");
                alert.showAndWait();
            }
        });

        VBox layout = new VBox(10);
        layout.setPadding(new javafx.geometry.Insets(15));
        layout.getChildren().addAll(
                idLabel, idField,
                fetchButton,
                new Separator(),
                nameLabel, nameField,
                dateLabel, dateField,
                amountLabel, amountField,
                categoryLabel, categoryField,
                descriptionLabel, descriptionField,
                deleteButton);

        Scene scene = new Scene(layout, 300, 450);
        formStage.setScene(scene);
        formStage.showAndWait();
        return result[0];
    }

    private void loadData() {
        tableView.getItems().clear();
        List<Expense> expenses = dbHandler.getAllExpenses(currentUserId);
        tableView.getItems().addAll(expenses);
    }

    private void updateChart(VBox rightSidebar, ChartService  chartService) {
        List<Expense> expenses = dbHandler.getAllExpenses(currentUserId);
        javafx.collections.ObservableList<Expense> observableExpenses = javafx.collections.FXCollections.observableArrayList(expenses);

        PieChart autoPieChart = chartService.createPieChart(observableExpenses);
        StackedBarChart<String, Number> autoStackedBarChart = chartService.createStackedBarChart(observableExpenses);

        Label chartTitle = new Label("Visualization:");
        chartTitle.setStyle("-fx-text-fill: #2c3e58; -fx-font-size: 16px; -fx-font-weight: bold");

        rightSidebar.getChildren().setAll(chartTitle, autoPieChart, autoStackedBarChart);
    }

    private boolean showLogin() {
        Stage loginStage = new Stage();
        loginStage.setTitle("Login or Register");

        Label usernameLabel = new Label("Username");
        TextField usernameField = new TextField();

        Label passwordLabel = new Label("Password");
        PasswordField passwordField = new PasswordField();

        Button loginButton = new Button("Login");
        Button registerButton = new Button("Register");

        final boolean[] loggedIn = { false };

        loginButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Please enter both username and password");
                return;
            }

            int userId = dbHandler.login(new User(null, username, password));
            if (userId != -1) {
                currentUserId = userId;
                loggedIn[0] = true;
                showAlert(Alert.AlertType.INFORMATION, "Login successful");
                loginStage.close();
            } else {
                showAlert(Alert.AlertType.ERROR, "Invalid username or password");
            }
        });

        registerButton.setOnAction(e -> {
            String username = usernameField.getText().trim();
            String password = passwordField.getText().trim();

            if (username.isEmpty() || password.isEmpty()) {
                showAlert(Alert.AlertType.ERROR, "Please enter both username and password");
                return;
            }

            boolean success = dbHandler.register(new User(null, username, password));
            if (success) {
                showAlert(Alert.AlertType.INFORMATION, "Registered successfully! You can now log in.");
            } else {
                showAlert(Alert.AlertType.ERROR, "Registration failed. Username might already exist.");
            }
        });

        HBox buttons = new HBox(10, loginButton, registerButton);
        VBox layout = new VBox(10, usernameLabel, usernameField, passwordLabel, passwordField, buttons);
        layout.setPadding(new javafx.geometry.Insets(15));

        loginStage.setScene(new Scene(layout, 300, 200));
        loginStage.showAndWait();

        return loggedIn[0];
    }

    private void showAlert(Alert.AlertType type, String message) {
        Alert alert = new Alert(type, message);
        alert.showAndWait();
    }

    public void logout() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Logout");
        alert.setHeaderText("Confirm Logout");
        alert.setContentText("Are you sure you want to log out?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            currentUserId = -1;
            System.out.println("User logged out successfully");

            Stage stage = (Stage) logoutButton.getScene().getWindow();
            stage.close();

            boolean loggedIn = showLogin();
            if (loggedIn) {
                System.out.println("User did not login. Exiting");
                System.exit(0);
            } else {
                start(new Stage());
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
