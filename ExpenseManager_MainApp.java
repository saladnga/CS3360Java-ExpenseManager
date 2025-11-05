
// Import SQL
import java.sql.SQLException;
import java.util.List;

// Import JavaFX
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;

// javac --module-path /Users/UserN/Desktop/TROY/CS-3360/project/lib --add-modules javafx.controls ExpenseManager_MainApp.java
// java --module-path /Users/UserN/Desktop/TROY/CS-3360/project/lib --add-modules javafx.controls -cp /Users/UserN/Desktop/TROY/CS-3360/project/lib/sqlite-jdbc-3.42.0.0.jar:. ExpenseManager_MainApp

public class ExpenseManager_MainApp extends Application {

    private DatabaseHandler dbHandler;
    private TableView<Expense> tableView;

    @Override
    public void start(Stage primaryStage) {
        dbHandler = new DatabaseHandler();
        CSVHandler csvHandler = new CSVHandler();

        try {
            dbHandler.connect();
        } catch (SQLException ex) {
            ex.printStackTrace();
            return;
        }

        BorderPane mainLayout = new BorderPane();

        // Sidebar
        VBox sidebar = new VBox(15);
        sidebar.setStyle("-fx-padding: 20; -fx-background-color: #2c3e58");
        sidebar.setPrefWidth(180);

        Label title = new Label("Menu");
        title.setStyle("-fx-text-fill: white; -fx-font-size: 16px; -fx-font-weight: bold");

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

        sidebar.getChildren().addAll(title, new Separator(), createButton, readButton, deleteButton, updateButton, exportButton, importButton);

        // Main Content
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
        });

        readButton.setOnAction(_ -> {
            loadData();
        });

        updateButton.setOnAction(_ -> {
            updateExpense();
            loadData();
        });

        deleteButton.setOnAction(_ -> {
            deleteExpense();
            loadData();
        });

        exportButton.setOnAction(_ -> {
            try {
                List<Expense> expenses = dbHandler.getAllExpenses();
                System.out.println("Exporting " + expenses.size() + " expenses");
                for (Expense e : expenses) {
                    System.out.println(e.getId() + " - " + e.getName());
                }
                csvHandler.writeCSV("expenses_export.csv", expenses);
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
                    dbHandler.saveExpense(expense);
                }
                loadData();
                Alert alert = new Alert(Alert.AlertType.INFORMATION, "Imported file");
                alert.showAndWait();
            } catch (Exception ex) {
                ex.printStackTrace();
                Alert alert = new Alert(Alert.AlertType.ERROR, "Failed to import file");
                alert.showAndWait();
            }
        });

        // Main Layout
        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);

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

                dbHandler.saveExpense(newExpense);
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
                Expense expenseToUpdate = dbHandler.getExpenseById(id);

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
                Expense expenseToUpdate = dbHandler.getExpenseById(id);

                if (expenseToUpdate != null) {
                    expenseToUpdate.setName(nameField.getText());
                    expenseToUpdate.setDate(dateField.getText());
                    expenseToUpdate.setAmount(amount);
                    expenseToUpdate.setCategory(categoryField.getText());
                    expenseToUpdate.setDescription(descriptionField.getText());

                    boolean updated = dbHandler.updateExpense(expenseToUpdate);
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
                Expense expenseToUpdate = dbHandler.getExpenseById(id);

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
                Expense expenseToDelete = dbHandler.getExpenseById(id);

                if (expenseToDelete != null) {

                    boolean deleted = dbHandler.deleteExpense(expenseToDelete);
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
        List<Expense> expenses = dbHandler.getAllExpenses();
        tableView.getItems().addAll(expenses);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
