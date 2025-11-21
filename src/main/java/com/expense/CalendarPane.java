package com.expense;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class CalendarPane extends VBox {

    private List<Expense> expenses;
    private ComboBox<String> monthBox;
    private ComboBox<Integer> yearBox;
    private GridPane calendarGrid;

    public CalendarPane(List<Expense> expenses) {
        this.expenses = expenses;

        setSpacing(10);
        setPadding(new Insets(10));

        // Month selector
        monthBox = new ComboBox<>();
        monthBox.getItems().addAll("01","02","03","04","05","06","07","08","09","10","11","12");
        monthBox.setValue(LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("MM")));

        // Year selector
        yearBox = new ComboBox<>();
        for (int y = 2020; y <= 2035; y++) yearBox.getItems().add(y);
        yearBox.setValue(LocalDate.now().getYear());

        monthBox.setOnAction(e -> updateCalendar());
        yearBox.setOnAction(e -> updateCalendar());

        HBox header = new HBox(10, new Label("Month:"), monthBox,
                new Label("Year:"), yearBox);
        header.setAlignment(Pos.CENTER);
        header.setPadding(new Insets(5));

        calendarGrid = new GridPane();
        calendarGrid.setHgap(5);
        calendarGrid.setVgap(5);

        getChildren().addAll(header, calendarGrid);

        updateCalendar();
    }

    private void updateCalendar() {
        calendarGrid.getChildren().clear();

        int month = Integer.parseInt(monthBox.getValue());
        int year = yearBox.getValue();
        YearMonth selected = YearMonth.of(year, month);

        LocalDate firstDay = selected.atDay(1);
        int daysInMonth = selected.lengthOfMonth();

        // Spending per day in THIS month
        Map<Integer, Double> spendingPerDay = expenses.stream()
                .filter(e -> {
                    try {
                        LocalDate d = LocalDate.parse(e.getDate());
                        return d.getYear() == year && d.getMonthValue() == month;
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .collect(Collectors.groupingBy(
                        e -> LocalDate.parse(e.getDate()).getDayOfMonth(),
                        Collectors.summingDouble(Expense::getDisplayAmount)
                ));

        // Top 3 highest spend days
        List<Integer> top3Days = spendingPerDay.entrySet().stream()
                .sorted((a, b) -> Double.compare(b.getValue(), a.getValue()))
                .limit(3)
                .map(Map.Entry::getKey)
                .toList();

        int startDayOfWeek = firstDay.getDayOfWeek().getValue(); // Monday=1 … Sunday=7
        int col = startDayOfWeek - 1;
        int row = 0;

        for (int day = 1; day <= daysInMonth; day++) {

            double spending = spendingPerDay.getOrDefault(day, 0.0);

            VBox cell = new VBox();
            cell.setAlignment(Pos.TOP_CENTER);
            cell.setPrefSize(75, 60);

            // Use CSS classes instead of inline colors so theme switching controls text color
            Label dayLabel = new Label(String.valueOf(day));
            dayLabel.getStyleClass().add("calendar-day-label");

            Label spendLabel = new Label(spending > 0 ? String.format("$%.2f", spending) : "");
            spendLabel.getStyleClass().add("calendar-spend-label");

            cell.getChildren().addAll(dayLabel, spendLabel);

            // Background color
            Background normalBg = new Background(new BackgroundFill(Color.WHITE, null, null));
            Background spendingBg = new Background(new BackgroundFill(Color.web("#e8f5ff"), null, null));
            Background topBg = new Background(new BackgroundFill(Color.web("#ffcccc"), null, null));

            if (top3Days.contains(day))
                cell.setBackground(topBg);
            else if (spending > 0)
                cell.setBackground(spendingBg);
            else
                cell.setBackground(normalBg);

            // Hover effect
            cell.setOnMouseEntered(ev -> {
                cell.setCursor(Cursor.HAND);
                cell.setBorder(new Border(new BorderStroke(
                        Color.web("#2c3e58"),
                        BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
                )));
            });

            cell.setOnMouseExited(ev -> {
                cell.setBorder(new Border(new BorderStroke(
                        Color.web("#ccc"),
                        BorderStrokeStyle.SOLID, CornerRadii.EMPTY, BorderWidths.DEFAULT
                )));
            });

            // CLICK EVENT → show detail window
            LocalDate clickedDate = LocalDate.of(year, month, day);
            cell.setOnMouseClicked(ev -> showDayDetails(clickedDate));

            calendarGrid.add(cell, col, row);

            col++;
            if (col > 6) {
                col = 0;
                row++;
            }
        }
    }

    // =========================================================
    // SHOW DAY DETAIL WINDOW (method must be OUTSIDE updateCalendar)
    // =========================================================
    private void showDayDetails(LocalDate date) {

        List<Expense> dayExpenses = expenses.stream()
                .filter(e -> {
                    try {
                        return LocalDate.parse(e.getDate()).equals(date);
                    } catch (Exception ex) {
                        return false;
                    }
                })
                .toList();

        Stage detailStage = new Stage();
        detailStage.setTitle("Expenses on " + date);

        VBox layout = new VBox(10);
        layout.setPadding(new Insets(15));

        Label title = new Label("Expenses on " + date);
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");

        TableView<Expense> table = new TableView<>();

        TableColumn<Expense, String> nameCol = new TableColumn<>("Name");
        nameCol.setCellValueFactory(new PropertyValueFactory<>("name"));

        TableColumn<Expense, Double> amountCol = new TableColumn<>("Amount");
        amountCol.setCellValueFactory(new PropertyValueFactory<>("displayAmount"));

        TableColumn<Expense, String> categoryCol = new TableColumn<>("Category");
        categoryCol.setCellValueFactory(new PropertyValueFactory<>("category"));

        TableColumn<Expense, String> descCol = new TableColumn<>("Description");
        descCol.setCellValueFactory(new PropertyValueFactory<>("description"));

        table.getColumns().addAll(nameCol, amountCol, categoryCol, descCol);
        table.getItems().addAll(dayExpenses);

        double total = dayExpenses.stream()
                .mapToDouble(Expense::getDisplayAmount)
                .sum();

        Label totalLabel = new Label("Total: " + String.format("%.2f", total));
        totalLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");

        layout.getChildren().addAll(title, table, totalLabel);

        Scene scene = new Scene(layout, 600, 400);
        detailStage.setScene(scene);
        detailStage.show();
    }
}
