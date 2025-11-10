import javafx.collections.ObservableList;
import javafx.scene.chart.*;

import java.util.Map;
import java.util.stream.Collectors;

public class ChartService {
    public PieChart createPieChart(ObservableList<Expense> expenses) {
        PieChart pieChart = new PieChart();
        pieChart.setTitle("Expenses by Category");
        pieChart.setLabelsVisible(true);
        pieChart.setClockwise(true);
        pieChart.setLegendVisible(true);

        Map<String, Double> categoryTotals = expenses.stream().collect(Collectors.groupingBy(Expense::getCategory, Collectors.summingDouble(Expense::getAmount)));

        double total = categoryTotals.values().stream().mapToDouble(Double::doubleValue).sum();

        for (Map.Entry<String, Double> entry: categoryTotals.entrySet()) {
            PieChart.Data data = new PieChart.Data(entry.getKey(), entry.getValue());
            pieChart.getData().add(data);
        }
        return pieChart;
    }

    public StackedBarChart<String, Number> createStackedBarChart(ObservableList<Expense> expenses) {
        CategoryAxis xAxis = new CategoryAxis();
        xAxis.setLabel("Month");

        NumberAxis yAxis = new NumberAxis();
        yAxis.setLabel("Total Amount");

        StackedBarChart<String, Number> stackedBarChart = new StackedBarChart<>(xAxis, yAxis);
        stackedBarChart.setTitle("Monthly Expenses");

        Map<String, Map<String, Double>> groupedData = expenses.stream().collect(Collectors.groupingBy(Expense::getCategory, Collectors.groupingBy(e -> e.getDate().substring(0,7), Collectors.summingDouble(Expense::getAmount))));

        for (Map.Entry<String, Map<String, Double>> categoryEntry : groupedData.entrySet()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName(categoryEntry.getKey());

            for (Map.Entry<String, Double> monthEntry : categoryEntry.getValue().entrySet()) {
                series.getData().add(new XYChart.Data<>(monthEntry.getKey(), monthEntry.getValue()));
            }

            stackedBarChart.getData().add(series);
        }
        return stackedBarChart;
    }
}
