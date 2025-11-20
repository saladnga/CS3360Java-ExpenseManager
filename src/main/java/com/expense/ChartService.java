package com.expense;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;

import java.util.Map;
import java.util.stream.Collectors;

public class ChartService {

    // SINGLE ZOOM WINDOW
    private Stage zoomStage = null;

    // =====================
    //  ZOOM WINDOW HANDLER
    // =====================
    private void openLargeChartWindow(javafx.scene.Node chartNode) {

        // If window is closed or null → create new
        if (zoomStage == null) {
            zoomStage = new Stage();
            zoomStage.setTitle("Chart Zoom");
            zoomStage.setOnCloseRequest(e -> zoomStage = null);
        }

        VBox root = new VBox(chartNode);
        root.setPadding(new Insets(20));

        Scene scene = new Scene(root, 900, 600);
        zoomStage.setScene(scene);

        zoomStage.show();
        zoomStage.toFront();
    }


    // =======================================================
    // 1. PIE CHART
    // =======================================================
    public PieChart createPieChart(ObservableList<Expense> expenses) {

        PieChart pieChart = new PieChart();
        pieChart.setTitle("Expenses by Category");
        pieChart.setLabelsVisible(true);

        Map<String, Double> totals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getDisplayAmount)
                ));

        totals.forEach((cat, amount) -> pieChart.getData().add(new PieChart.Data(cat, amount)));

        // ZOOM EVENT
        pieChart.setOnMouseClicked(e -> {
            PieChart zoomPie = createPieChart(expenses); // fresh copy
            openLargeChartWindow(zoomPie);
        });

        return pieChart;
    }


    // =======================================================
    // 2. STACKED BAR CHART
    // =======================================================
    public StackedBarChart<String, Number> createStackedBarChart(ObservableList<Expense> expenses) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Amount");

        StackedBarChart<String, Number> chart = new StackedBarChart<>(xAxis, yAxis);
        chart.setTitle("Monthly Expenses");

        Map<String, Map<String, Double>> grouped =
                expenses.stream().collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.groupingBy(
                                e -> e.getDate().substring(0, 7),
                                Collectors.summingDouble(Expense::getDisplayAmount)
                        )));

        grouped.forEach((category, monthData) -> {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(category);

            monthData.forEach((month, amount) ->
                    series.getData().add(new XYChart.Data<>(month, amount)));

            chart.getData().add(series);
        });

        chart.setOnMouseClicked(e -> {
            var zoomChart = createStackedBarChart(expenses);
            openLargeChartWindow(zoomChart);
        });

        return chart;
    }


    // =======================================================
    // 3. BAR CHART
    // =======================================================
    public BarChart<String, Number> createBarChart(ObservableList<Expense> expenses) {

        Map<String, Double> totals = expenses.stream()
                .collect(Collectors.groupingBy(
                        Expense::getCategory,
                        Collectors.summingDouble(Expense::getDisplayAmount)
                ));

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Category");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Amount");

        BarChart<String, Number> chart = new BarChart<>(xAxis, yAxis);
        chart.setTitle("Total Spending by Category");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Totals");

        totals.forEach((category, sum) -> series.getData().add(new XYChart.Data<>(category, sum)));

        chart.getData().add(series);

        chart.setOnMouseClicked(e -> {
            var zoomChart = createBarChart(expenses);
            openLargeChartWindow(zoomChart);
        });

        return chart;
    }


    // =======================================================
    // 4. LINE CHART
    // =======================================================
    public LineChart<String, Number> createLineChart(ObservableList<Expense> expenses) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis();

        LineChart<String, Number> chart = new LineChart<>(xAxis, yAxis);
        chart.setTitle("com.expense.Expense Trend Over Time");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Spending");

        expenses.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(e.getDate(), e.getDisplayAmount())
                ));

        chart.getData().add(series);

        chart.setOnMouseClicked(e -> {
            var zoom = createLineChart(expenses);
            openLargeChartWindow(zoom);
        });

        return chart;
    }


    // =======================================================
    // 5. AREA CHART
    // =======================================================
    public AreaChart<String, Number> createAreaChart(ObservableList<Expense> expenses) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis();

        AreaChart<String, Number> chart = new AreaChart<>(xAxis, yAxis);
        chart.setTitle("com.expense.Expense Area Distribution");

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Daily Spending");

        expenses.stream()
                .sorted((a, b) -> a.getDate().compareTo(b.getDate()))
                .forEach(e -> series.getData().add(
                        new XYChart.Data<>(e.getDate(), e.getDisplayAmount())
                ));

        chart.getData().add(series);

        chart.setOnMouseClicked(e -> {
            var zoom = createAreaChart(expenses);
            openLargeChartWindow(zoom);
        });

        return chart;
    }


    // =======================================================
    // 6. SCATTER CHART
    // =======================================================
    public ScatterChart<String, Number> createScatterChart(ObservableList<Expense> expenses) {

        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Date");

        NumberAxis yAxis = new NumberAxis();

        ScatterChart<String, Number> chart = new ScatterChart<>(xAxis, yAxis);
        chart.setTitle("com.expense.Expense Scatter Plot");

        XYChart.Series<String, Number> series = new XYChart.Series<>();

        expenses.forEach(e -> series.getData().add(
                new XYChart.Data<>(e.getDate(), e.getDisplayAmount())
        ));

        chart.getData().add(series);

        chart.setOnMouseClicked(e -> {
            var zoom = createScatterChart(expenses);
            openLargeChartWindow(zoom);
        });

        return chart;
    }


    // =======================================================
    // 7. DONUT CHART
    // =======================================================
    public StackPane createDonutChart(ObservableList<Expense> expenses) {

        PieChart pie = createPieChart(expenses);
        Circle hole = new Circle(60, Color.WHITE);

        StackPane pane = new StackPane(pie, hole);

        pane.setOnMouseClicked(e -> {
            var zoom = createDonutChart(expenses);
            openLargeChartWindow(zoom);
        });

        return pane;
    }
}
