package com.expense;// Import SQL

import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

// Import JavaFX
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.geometry.Insets;
import javafx.geometry.Pos;

import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileWriter;
import java.io.FileOutputStream;

import org.json.JSONObject;

// PDF
import com.lowagie.text.pdf.PdfWriter;

// Excel
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

// Robot Animation
import javafx.animation.Animation;
import javafx.animation.FadeTransition;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.Text;

// Compile: mvn -q clean compile
// Run: mvn -q javafx:run

public class ExpenseManager_MainApp extends Application {

    private DatabaseHandler dbHandler;
    private CSVHandler csvHandler;
    private TableView<Expense> tableView;
    private Integer currentUserId = -1;
    private Button logoutButton;
    private VBox rightSidebar;
    private String currentCurrency = "USD";

    // ===== CURRENCY =====
    private final CurrencyConverter converter = new CurrencyConverter();
    private String selectedCurrency = "USD";
    private HBox summaryCardBox; // store summary cards for refresh

    // ===== CATEGORY NORMALIZATION =====
    private static final Map<String, String> CATEGORY_MAP = new HashMap<>();
    private static final List<String> CATEGORY_LIST = Arrays.asList(
            "Food & Drinks", "Utilities", "Personal Care", "Entertainment",
            "Education", "Health", "Transportation", "Electronics", "Sports", "Food");

    static {
        CATEGORY_MAP.put("food", "Food & Drinks");
        CATEGORY_MAP.put("foods", "Food & Drinks");
        CATEGORY_MAP.put("drink", "Food & Drinks");
        CATEGORY_MAP.put("drinks", "Food & Drinks");
        CATEGORY_MAP.put("meal", "Food & Drinks");
        CATEGORY_MAP.put("food & drinks", "Food & Drinks");

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
        CATEGORY_MAP.put("helth", "Health");
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

    // Normalize user-typed category
    private String normalizeCategory(String input) {
        if (input == null || input.trim().isEmpty())
            return "Other";

        input = input.toLowerCase().trim();

        if (CATEGORY_MAP.containsKey(input))
            return CATEGORY_MAP.get(input);

        String bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (String key : CATEGORY_MAP.keySet()) {
            int dist = levenshteinDistance(input, key);
            if (dist < bestDistance) {
                bestDistance = dist;
                bestMatch = key;
            }
        }

        if (bestDistance <= 2) {
            return CATEGORY_MAP.get(bestMatch);
        }

        return "Other";
    }

    private int levenshteinDistance(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];

        for (int i = 0; i <= a.length(); i++)
            dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++)
            dp[0][j] = j;

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

    // Convert amount from USD to selected currency
    private double convert(double amountUSD) {
        return converter.convertCurrency(amountUSD, "USD", selectedCurrency);
    }

    // =====================================================================
    @Override
    public void start(Stage primaryStage) {
        dbHandler = new DatabaseHandler();
        csvHandler = new CSVHandler();
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

        ReportGenerator reportGenerator = new ReportGenerator(dbHandler.getConnection());

        boolean loggedIn = showLogin();
        if (!loggedIn) {
            System.out.println("com.expense.User did not login. Exiting.");
            return;
        }

        BorderPane mainLayout = new BorderPane();

        // Sidebar
        VBox sidebar = new VBox(15);
        // use CSS class so themes control background and text color
        sidebar.getStyleClass().add("sidebar");
        sidebar.setPrefWidth(180);

        Label title = new Label("Menu");
        title.getStyleClass().add("sidebar-title");

        Button createButton = new Button("Create Expense");
        Button readButton = new Button("Refresh");
        Button updateButton = new Button("Update Expense");
        Button deleteButton = new Button("Delete Expense");
        Button importButton = new Button("Import CSV");
        Button reportButton = new Button("Generate Report");
        reportButton.setPrefWidth(150);

        // CLEAR ALL DATA BUTTON
        Button clearButton = new Button("Clear All Data");
        clearButton.setPrefWidth(150);

        clearButton.setOnAction(e -> {
            dbHandler.deleteAllExpenses(currentUserId);
            dbHandler.resetAutoIncrement();
            loadData();
            refreshCurrencyUI();
        });

        for (Button b : Arrays.asList(createButton, readButton, updateButton, deleteButton, importButton)) {
            b.setPrefWidth(150);
        }

        // ===== CURRENCY SELECTOR =====
        Label currencyLabel = new Label("Currency:");
        currencyLabel.getStyleClass().add("sidebar-label");
        ComboBox<String> currencySelector = new ComboBox<>();
        currencySelector.getItems().addAll("USD", "EUR", "VND", "JPY", "GBP");
        currencySelector.setValue("USD");
        currencySelector.setPrefWidth(150);

        currencySelector.setOnAction(e -> {
            selectedCurrency = currencySelector.getValue();
            refreshCurrencyUI();
        });

        // Dark mode toggle
        ToggleButton darkMode = new ToggleButton("Dark mode");
        darkMode.setPrefWidth(150);
        darkMode.setOnAction(e -> {
            Scene scene = logoutButton.getScene();
            if (darkMode.isSelected()) {
                scene.getStylesheets().setAll(getClass().getResource("/dark.css").toExternalForm());
                darkMode.setText("Light mode");
            } else {
                scene.getStylesheets().setAll(getClass().getResource("/app.css").toExternalForm());
                darkMode.setText("Dark mode");
            }
        });

        sidebar.getChildren().addAll(title, new Separator(),
                createButton, readButton, updateButton, deleteButton, importButton,
                currencyLabel, currencySelector,
                reportButton,
                clearButton,
                darkMode,
                logoutButton);

        // ============================
        // CHART SIDEBAR + SELECTOR
        // ============================
        ChartService chartService = new ChartService();
        rightSidebar = new VBox(15);
        rightSidebar.getStyleClass().add("right-sidebar");
        rightSidebar.setPadding(new Insets(15));
        // align children to the left so the "Visualization" label lines up with center
        // content
        rightSidebar.setAlignment(Pos.TOP_LEFT);

        Label chartTitle = new Label("Visualization");
        // use CSS class so themes can control size/color
        chartTitle.getStyleClass().add("section-title");
        // expand label width and align text to left so it matches the center title
        chartTitle.setMaxWidth(Double.MAX_VALUE);
        chartTitle.setAlignment(Pos.BASELINE_LEFT);

        ComboBox<String> chartSelector = new ComboBox<>();
        chartSelector.getItems().addAll(
                "Pie Chart",
                "Stacked Bar Chart",
                "Bar Chart",
                "Line Chart",
                "Area Chart",
                "Scatter Chart",
                "Donut Chart");
        chartSelector.setValue("Pie Chart");

        StackPane chartContainer = new StackPane();
        chartContainer.setPrefHeight(400);
        chartContainer.getStyleClass().add("chart-container");

        Runnable updateChartUI = () -> {
            chartContainer.getChildren().clear();
            var data = javafx.collections.FXCollections.observableArrayList(
                    dbHandler.getAllExpenses(currentUserId));

            // Apply currency conversion to expense objects
            for (Expense ex : data) {
                ex.setDisplayAmount(convert(ex.getAmount()));
            }

            switch (chartSelector.getValue()) {
                case "Pie Chart":
                    chartContainer.getChildren().add(chartService.createPieChart(data));
                    break;
                case "Stacked Bar Chart":
                    chartContainer.getChildren().add(chartService.createStackedBarChart(data));
                    break;
                case "Bar Chart":
                    chartContainer.getChildren().add(chartService.createBarChart(data));
                    break;
                case "Line Chart":
                    chartContainer.getChildren().add(chartService.createLineChart(data));
                    break;
                case "Area Chart":
                    chartContainer.getChildren().add(chartService.createAreaChart(data));
                    break;
                case "Scatter Chart":
                    chartContainer.getChildren().add(chartService.createScatterChart(data));
                    break;
                case "Donut Chart":
                    chartContainer.getChildren().add(chartService.createDonutChart(data));
                    break;
            }
        };

        chartSelector.setOnAction(e -> updateChartUI.run());

        CalendarPane calendarPane = new CalendarPane(dbHandler.getAllExpenses(currentUserId));

        rightSidebar.getChildren().addAll(
                chartTitle,
                chartSelector,
                chartContainer,
                new Separator(),
                calendarPane);

        updateChartUI.run();

        // ============================
        // TABLE + SUMMARY CARDS
        // ============================
        VBox content = new VBox(10);
        content.setPadding(new Insets(15));
        // ensure center content children align to the left as well
        content.setAlignment(Pos.TOP_LEFT);

        Label contentTitle = new Label("All Expenses");
        // match chart title via CSS
        contentTitle.getStyleClass().add("section-title");
        contentTitle.setMaxWidth(Double.MAX_VALUE);
        contentTitle.setAlignment(Pos.BASELINE_LEFT);

        tableView = new TableView<>();
        tableView.getStyleClass().add("table-view");
        // Make columns resize to fill the table width so the Description column can
        // take
        // remaining space instead of leaving unused blank area.
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Expense, Integer> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(new PropertyValueFactory<>("id"));

        TableColumn<Expense, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Expense, String> dateCol = new TableColumn<>("Date");
        dateCol.setCellValueFactory(new PropertyValueFactory<>("date"));

        // Show converted displayAmount
        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("displayAmount"));

        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        // Make description column wrap text and allow row height to expand
        descCol.setCellFactory(col -> {
            TableCell<Expense, String> cell = new TableCell<>() {
                private final Text text = new Text();
                {
                    text.wrappingWidthProperty().bind(col.widthProperty().subtract(10));
                    setPrefHeight(Control.USE_COMPUTED_SIZE);
                }

                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setGraphic(null);
                        setText(null);
                    } else {
                        text.setText(item);
                        setGraphic(text);
                    }
                }
            };
            return cell;
        });

        // Set reasonable pref widths for other columns so Description naturally gets
        // the rest
        idCol.setPrefWidth(50);
        nameCol.setPrefWidth(150);
        dateCol.setPrefWidth(110);
        amountCol.setPrefWidth(100);
        categoryCol.setPrefWidth(130);
        // Allow description column to expand fully
        descCol.setMaxWidth(Double.MAX_VALUE);

        tableView.getColumns().addAll(idCol, nameCol, dateCol, amountCol, categoryCol, descCol);

        loadData();
        summaryCardBox = createSummaryCards();

        content.getChildren().addAll(summaryCardBox, contentTitle, tableView);

        // ===== ROBOT ANIMATION ZONE =====
        Pane robotPane = createRobotPane();
        content.getChildren().add(robotPane);

        // CRUD Events
        createButton.setOnAction(e -> {
            createExpense();
            loadData();
            refreshCurrencyUI();
            updateChartUI.run();
        });

        readButton.setOnAction(e -> {
            loadData();
            refreshCurrencyUI();
            updateChartUI.run();
        });

        updateButton.setOnAction(e -> {
            updateExpense();
            loadData();
            refreshCurrencyUI();
            updateChartUI.run();
        });

        deleteButton.setOnAction(e -> {
            deleteExpense();
            loadData();
            refreshCurrencyUI();
            updateChartUI.run();
        });

        reportButton.setOnAction(e -> showReportWindow());

        importButton.setOnAction(e -> {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Import CSV");
            chooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

            File selectedFile = chooser.showOpenDialog(null);
            if (selectedFile == null)
                return;

            try {
                List<Expense> imported = csvHandler.readCSV(selectedFile.getAbsolutePath());

                for (Expense ex : imported) {
                    ex.setCategory(normalizeCategory(ex.getCategory()));
                    dbHandler.saveExpense(ex, currentUserId);
                }

                loadData();
                refreshCurrencyUI();
                updateChartUI.run();

                showAlert(Alert.AlertType.INFORMATION, "Imported successfully!");

            } catch (Exception err) {
                err.printStackTrace();
                showAlert(Alert.AlertType.ERROR, "Failed to import CSV.");
            }
        });

        mainLayout.setLeft(sidebar);
        mainLayout.setCenter(content);
        mainLayout.setRight(rightSidebar);

        Scene scene = new Scene(mainLayout, 1100, 650);
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        primaryStage.setScene(scene);
        primaryStage.setTitle("Expense Manager");
        primaryStage.show();
    }

    // Refresh UI when switching currency
    private void refreshCurrencyUI() {
        for (Expense ex : tableView.getItems()) {
            ex.setDisplayAmount(convert(ex.getAmount()));
        }
        tableView.refresh();

        summaryCardBox.getChildren().setAll(createSummaryCards().getChildren());

        // === REFRESH CALENDAR ===
        CalendarPane newCalendar = new CalendarPane(dbHandler.getAllExpenses(currentUserId));
        rightSidebar.getChildren().set(rightSidebar.getChildren().size() - 1, newCalendar);
    }

    // ============= CREATE EXPENSE =============
    private void createExpense() {
        Stage form = new Stage();
        form.setTitle("Create Expense");

        Label nameL = new Label("Name:");
        TextField nameF = new TextField();

        Label dateL = new Label("Date (YYYY-MM-DD):");
        TextField dateF = new TextField();

        Label amountL = new Label("Amount:");
        TextField amountF = new TextField();

        Label categoryL = new Label("Category:");
        TextField categoryF = new TextField();

        Label descL = new Label("Description");
        TextField descF = new TextField();

        Button save = new Button("Save");

        save.setOnAction(e -> {
            try {
                double amount = Double.parseDouble(amountF.getText());
                String categoryNormalized = normalizeCategory(categoryF.getText());

                Expense ex = new Expense(null,
                        dateF.getText(),
                        nameF.getText(),
                        amount,
                        categoryNormalized,
                        descF.getText());

                dbHandler.saveExpense(ex, currentUserId);
                // refresh UI immediately so user sees the new expense without clicking "View
                // All Expenses"
                loadData();
                refreshCurrencyUI();
                form.close();
            } catch (Exception err) {
                showAlert(Alert.AlertType.ERROR, "Invalid input!");
            }
        });

        VBox layout = new VBox(12, nameL, nameF, dateL, dateF, amountL, amountF,
                categoryL, categoryF, descL, descF, save);
        layout.setPadding(new Insets(15));

        form.setScene(new Scene(layout, 400, 420));
        form.show();
    }

    // ============= UPDATE EXPENSE =============
    private void updateExpense() {
        Stage form = new Stage();
        form.setTitle("Update Expense");

        TextField idF = new TextField();
        TextField nameF = new TextField();
        TextField dateF = new TextField();
        TextField amountF = new TextField();
        TextField categoryF = new TextField();
        TextField descF = new TextField();

        Button fetch = new Button("Fetch");
        Button save = new Button("Save");

        fetch.setOnAction(e -> {
            try {
                Expense ex = dbHandler.getExpenseById(Integer.parseInt(idF.getText()), currentUserId);
                if (ex == null) {
                    showAlert(Alert.AlertType.ERROR, "Expense not found");
                    return;
                }

                nameF.setText(ex.getName());
                dateF.setText(ex.getDate());
                amountF.setText(String.valueOf(ex.getAmount()));
                categoryF.setText(ex.getCategory());
                descF.setText(ex.getDescription());
            } catch (Exception err) {
                showAlert(Alert.AlertType.ERROR, "Invalid ID");
            }
        });

        save.setOnAction(e -> {
            try {
                int id = Integer.parseInt(idF.getText());
                double amount = Double.parseDouble(amountF.getText());

                Expense ex = dbHandler.getExpenseById(id, currentUserId);

                ex.setName(nameF.getText());
                ex.setDate(dateF.getText());
                ex.setAmount(amount);
                ex.setCategory(normalizeCategory(categoryF.getText()));
                ex.setDescription(descF.getText());

                dbHandler.updateExpense(ex, currentUserId);
                form.close();

            } catch (Exception err) {
                showAlert(Alert.AlertType.ERROR, "Invalid data");
            }
        });

        VBox layout = new VBox(15, new Label("ID"), idF, fetch,
                new Label("Name"), nameF,
                new Label("Date"), dateF,
                new Label("Amount"), amountF,
                new Label("Category"), categoryF,
                new Label("Description"), descF,
                save);
        layout.setPadding(new Insets(15));

        form.setScene(new Scene(layout, 350, 500));
        form.show();
    }

    // ============= DELETE EXPENSE =============
    private void deleteExpense() {
        Stage form = new Stage();
        form.setTitle("Delete Expense");

        TextField idF = new TextField();

        Button delete = new Button("Delete");

        delete.setOnAction(e -> {
            try {
                int id = Integer.parseInt(idF.getText());
                Expense ex = dbHandler.getExpenseById(id, currentUserId);

                if (ex == null) {
                    showAlert(Alert.AlertType.ERROR, "Expense not found");
                    return;
                }

                dbHandler.deleteExpense(ex, currentUserId);
                form.close();

            } catch (Exception err) {
                showAlert(Alert.AlertType.ERROR, "Invalid ID");
            }
        });

        VBox layout = new VBox(10, new Label("Expense ID"), idF, delete);
        layout.setPadding(new Insets(15));

        form.setScene(new Scene(layout, 250, 150));
        form.show();
    }

    private void loadData() {
        List<Expense> list = dbHandler.getAllExpenses(currentUserId);

        // Apply converted currency
        for (Expense ex : list) {
            double converted = converter.convertCurrency(ex.getAmount(), "USD", currentCurrency);
            ex.setDisplayAmount(converted);
        }

        tableView.getItems().setAll(list);

        CalendarPane newCalendar = new CalendarPane(list);
        rightSidebar.getChildren().set(rightSidebar.getChildren().size() - 1, newCalendar);
    }

    // Login
    private boolean showLogin() {
        Stage login = new Stage();
        login.setTitle("Login");

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        Button loginBtn = new Button("Login");
        Button registerBtn = new Button("Register");

        final boolean[] result = { false };

        loginBtn.setOnAction(e -> {
            int id = dbHandler.login(new User(null, username.getText(), password.getText()));
            if (id != -1) {
                currentUserId = id;
                result[0] = true;
                login.close();
            } else
                showAlert(Alert.AlertType.ERROR, "Wrong username or password");
        });

        registerBtn.setOnAction(e -> showRegister());

        VBox layout = new VBox(10,
                new Label("Username"), username,
                new Label("Password"), password,
                new HBox(10, loginBtn, registerBtn));
        layout.setPadding(new Insets(15));

        login.setScene(new Scene(layout, 300, 200));
        login.showAndWait();

        return result[0];
    }

    private void showRegister() {
        Stage reg = new Stage();
        reg.setTitle("Register");

        TextField username = new TextField();
        PasswordField password = new PasswordField();

        Button register = new Button("Create Account");

        register.setOnAction(e -> {
            boolean ok = dbHandler.register(new User(null, username.getText(), password.getText()));
            if (ok) {
                showAlert(Alert.AlertType.INFORMATION, "Registered! Please login.");
                reg.close();
            } else
                showAlert(Alert.AlertType.ERROR, "Username exists!");
        });

        VBox layout = new VBox(10,
                new Label("New Username"), username,
                new Label("New Password"), password,
                register);
        layout.setPadding(new Insets(15));

        reg.setScene(new Scene(layout, 300, 200));
        reg.show();
    }

    private void importCSV() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select CSV File to Import");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));

        File file = chooser.showOpenDialog(null);
        if (file == null)
            return;

        try {
            List<Expense> csvList = csvHandler.readCSV(file.getAbsolutePath());

            for (Expense ex : csvList) {
                ex.setCategory(normalizeCategory(ex.getCategory()));
                dbHandler.saveExpense(ex, currentUserId);
            }

            loadData();
            refreshCurrencyUI();
            showAlert(Alert.AlertType.INFORMATION, "Imported successfully!");

        } catch (Exception e) {
            showAlert(Alert.AlertType.ERROR, "Failed to import file!");
            e.printStackTrace();
        }
    }

    private HBox createSummaryCards() {
        List<Expense> expenses = dbHandler.getAllExpenses(currentUserId);

        double total = expenses.stream()
                .mapToDouble(ex -> convert(ex.getAmount()))
                .sum();

        String topCategory = expenses.stream()
                .collect(Collectors.groupingBy(Expense::getCategory,
                        Collectors.summingDouble(Expense::getAmount)))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("N/A");

        double avg = expenses.isEmpty() ? 0 : total / expenses.size();

        VBox card1 = dashboardCard("Total Spent (" + selectedCurrency + ")",
                String.format("%.2f", total));

        VBox card2 = dashboardCard("Top Category", topCategory);

        VBox card3 = dashboardCard("Average Expense (" + selectedCurrency + ")",
                String.format("%.2f", avg));

        HBox box = new HBox(15, card1, card2, card3);
        box.setPadding(new Insets(10));

        return box;
    }

    private VBox dashboardCard(String title, String value) {
        Label t = new Label(title);
        // use CSS classes so themes can style titles and values
        t.getStyleClass().add("card-title");

        Label v = new Label(value);
        v.getStyleClass().add("card-value");

        VBox card = new VBox(5, t, v);
        card.getStyleClass().add("summary-card");
        card.setPrefWidth(150);
        return card;
    }

    private void showAlert(Alert.AlertType type, String msg) {
        Alert a = new Alert(type, msg);
        a.showAndWait();
    }

    private void showReportWindow() {
        ReportGenerator reportGen = new ReportGenerator(dbHandler.getConnection());

        Map<String, Double> monthly = reportGen.generateMonthlySummary();
        Map<String, Double> category = reportGen.generateCategorySummary();
        double total = reportGen.generateTotalSummary();

        String reportText = buildReportText(monthly, category, total);

        TextArea area = new TextArea(reportText);
        area.setEditable(false);

        ComboBox<String> formatBox = new ComboBox<>();
        formatBox.getItems().addAll("TXT", "CSV", "PDF", "Excel (XLSX)", "JSON");
        formatBox.setValue("TXT");

        Button exportBtn = new Button("Export");
        exportBtn.setOnAction(e -> {
            String format = formatBox.getValue();
            exportReport(format, reportText, monthly, category, total);
        });

        VBox root = new VBox(10, area, formatBox, exportBtn);
        root.setPadding(new Insets(10));

        Stage stage = new Stage();
        stage.setTitle("Expense Report");
        stage.setScene(new Scene(root, 500, 600));
        stage.show();
    }

    private String buildReportText(Map<String, Double> monthly,
            Map<String, Double> category,
            double total) {

        StringBuilder sb = new StringBuilder();
        sb.append("=== Monthly Summary ===\n");
        monthly.forEach((m, v) -> sb.append(m).append(": ").append(v).append("\n"));

        sb.append("\n=== Category Summary ===\n");
        category.forEach((c, v) -> sb.append(c).append(": ").append(v).append("\n"));

        sb.append("\n=== Total Expenses ===\n");
        sb.append(total).append("\n");

        return sb.toString();
    }

    private void exportReport(String format, String text,
            Map<String, Double> monthly,
            Map<String, Double> category,
            double total) {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Save Report");

        switch (format) {
            case "TXT":
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Text File", "*.txt"));
                saveText(chooser.showSaveDialog(null), text);
                break;

            case "CSV":
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("CSV File", "*.csv"));
                saveCSV(chooser.showSaveDialog(null), monthly, category, total);
                break;

            case "PDF":
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF File", "*.pdf"));
                savePDF(chooser.showSaveDialog(null), text);
                break;

            case "Excel (XLSX)":
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel File", "*.xlsx"));
                saveExcel(chooser.showSaveDialog(null), monthly, category, total);
                break;

            case "JSON":
                chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON File", "*.json"));
                saveJSON(chooser.showSaveDialog(null), monthly, category, total);
                break;
        }
    }

    private void saveText(File file, String text) {
        if (file == null)
            return;

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(text);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveCSV(File file,
            Map<String, Double> monthly,
            Map<String, Double> category,
            double total) {

        if (file == null)
            return;

        try (FileWriter fw = new FileWriter(file)) {
            fw.write("Section,Key,Value\n");

            for (var e : monthly.entrySet()) {
                fw.write("Monthly," + e.getKey() + "," + e.getValue() + "\n");
            }

            for (var e : category.entrySet()) {
                fw.write("Category," + e.getKey() + "," + e.getValue() + "\n");
            }

            fw.write("Total,All," + total + "\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void savePDF(File file, String text) {
        if (file == null)
            return;

        try {
            com.lowagie.text.Document doc = new com.lowagie.text.Document();
            PdfWriter.getInstance(doc, new FileOutputStream(file));
            doc.open();
            doc.add(new com.lowagie.text.Paragraph(text));
            doc.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveExcel(File file,
            Map<String, Double> monthly,
            Map<String, Double> category,
            double total) {

        if (file == null)
            return;

        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Report");

            int rowIndex = 0;

            // Monthly
            Row title1 = sheet.createRow(rowIndex++);
            title1.createCell(0).setCellValue("Monthly Summary");

            for (var e : monthly.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue());
            }

            rowIndex++;

            // Category
            Row title2 = sheet.createRow(rowIndex++);
            title2.createCell(0).setCellValue("Category Summary");

            for (var e : category.entrySet()) {
                Row row = sheet.createRow(rowIndex++);
                row.createCell(0).setCellValue(e.getKey());
                row.createCell(1).setCellValue(e.getValue());
            }

            rowIndex++;

            // Total
            Row title3 = sheet.createRow(rowIndex++);
            title3.createCell(0).setCellValue("Total Expenses");
            Row totalRow = sheet.createRow(rowIndex++);
            totalRow.createCell(0).setCellValue("Total");
            totalRow.createCell(1).setCellValue(total);

            // Save to file
            FileOutputStream fos = new FileOutputStream(file);
            workbook.write(fos);
            fos.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveJSON(File file,
            Map<String, Double> monthly,
            Map<String, Double> category,
            double total) {

        if (file == null)
            return;

        JSONObject json = new JSONObject();
        json.put("monthly", monthly);
        json.put("category", category);
        json.put("total", total);

        try (FileWriter fw = new FileWriter(file)) {
            fw.write(json.toString(4));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void logout() {
        Stage stage = (Stage) logoutButton.getScene().getWindow();
        stage.close();
        start(new Stage());
    }

    public static void main(String[] args) {
        launch(args);
    }

    private Pane createRobotPane() {

        Pane pane = new Pane();
        pane.setPrefHeight(220);

        // Áp dụng CSS class thay vì inline style
        pane.getStyleClass().add("robot-pane");

        // ===== Load robot quyền 200px =====
        Image robotImg = new Image(getClass().getResource("/robot_animation.png").toExternalForm());
        ImageView robot = new ImageView(robotImg);
    // slightly smaller robot so it fits better in narrow layouts
    robot.setFitWidth(140);
    robot.setFitHeight(140);

        // Vị trí robot dưới-left
    robot.setLayoutX(40);
    robot.setLayoutY(80);

        // ===== Bubble =====
        Label bubble = new Label("A penny saved is a penny earned");
        bubble.getStyleClass().add("robot-bubble");

        bubble.setLayoutX(robot.getLayoutX() + 10);
        bubble.setLayoutY(robot.getLayoutY() - 35);

        // ===== Animation =====
        TranslateTransition move = new TranslateTransition(Duration.seconds(5), robot);
    move.setFromX(0);
    // reduce horizontal travel so the robot movement is narrower
    move.setToX(180);
        move.setCycleCount(Animation.INDEFINITE);
        move.setAutoReverse(true);
        move.play();

        TranslateTransition moveBubble = new TranslateTransition(Duration.seconds(5), bubble);
    moveBubble.setFromX(0);
    moveBubble.setToX(180);
        moveBubble.setCycleCount(Animation.INDEFINITE);
        moveBubble.setAutoReverse(true);
        moveBubble.play();

        FadeTransition ft = new FadeTransition(Duration.seconds(1.1), bubble);
        ft.setFromValue(1);
        ft.setToValue(0.55);
        ft.setCycleCount(Animation.INDEFINITE);
        ft.setAutoReverse(true);
        ft.play();

        pane.getChildren().addAll(robot, bubble);
        return pane;
    }
}
