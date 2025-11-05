import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import java.util.List;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class CSVHandler {
    private final List<Expense> dataList = FXCollections.observableArrayList();

    public List<Expense> readCSV(String csvFile) {
        String fieldDelimiter = ",";
        dataList.clear();

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))){
            String line;
            boolean isHeader = true;
            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = line.split(fieldDelimiter, -1);
                if (fields.length < 6) continue;

                try {
                    Integer id = Integer.parseInt(fields[0].trim());
                    String date = fields[1].trim();
                    String name = fields[2].trim();
                    Double amount = Double.parseDouble(fields[3].trim());
                    String category = fields[4].trim();
                    String description = fields[5].trim();

                    Expense expense = new Expense(id, date, name, amount, category, description);
                    dataList.add(expense);
                } catch (NumberFormatException ex) {
                    Logger.getLogger(CSVHandler.class.getName()).log(Level.WARNING, "Skipping invalid line: " + line, ex);
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CSVHandler.class.getName())
                    .log(Level.SEVERE, null, ex);
        }
        return dataList;
    }

    public void writeCSV(String csvFile, List<Expense> expenses) {
        String fieldDelimiter = ",";

        try (BufferedWriter wr = new BufferedWriter(new FileWriter(csvFile))) {
            wr.write("ID,Date,Name,Amount,Category,Description");
            wr.newLine();
            for (Expense expense : expenses) {
                String line = String.join(fieldDelimiter,
                        expense.getId().toString(),
                        expense.getDate(),
                        expense.getName(),
                        String.valueOf(expense.getAmount()),
                        expense.getCategory(),
                        expense.getDescription()
                );
                wr.write(line);
                wr.newLine();
            }
        }  catch (IOException ex) {
            Logger.getLogger(CSVHandler.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}