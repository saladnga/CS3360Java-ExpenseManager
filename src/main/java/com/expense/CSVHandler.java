package com.expense;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CSVHandler {

    // Read CSV file and return list of Expense objects
    public List<Expense> readCSV(String csvFile) throws IOException {

        List<Expense> result = new ArrayList<>();
        String delimiter = ",";

        try (BufferedReader br = new BufferedReader(new FileReader(csvFile))) {

            String line;
            boolean isHeader = true;

            while ((line = br.readLine()) != null) {
                if (line.trim().isEmpty()) continue;

                // Skip header line
                if (isHeader) {
                    isHeader = false;
                    continue;
                }

                String[] fields = line.split(delimiter, -1);

                // Expected: Date, Name, Amount, Category, Description
                if (fields.length < 4) continue;

                String date = fields[0].trim();
                String name = fields[1].trim();
                double amount = Double.parseDouble(fields[2].trim());
                String category = fields[3].trim();
                String description = fields.length > 4 ? fields[4].trim() : "";

                // No ID used, SQLite will generate automatically
                Expense ex = new Expense(null, date, name, amount, category, description);
                result.add(ex);
            }
        }

        return result;
    }

    // Write CSV using clean export format
    @SuppressWarnings("unused")
    public void writeCSV(String csvFile, List<Expense> expenses) throws IOException {

        try (BufferedWriter bw = new BufferedWriter(new FileWriter(csvFile))) {

            // Header
            bw.write("Date,Name,Amount,Category,Description");
            bw.newLine();

            for (Expense e : expenses) {
                bw.write(e.getDate() + "," +
                        e.getName() + "," +
                        e.getAmount() + "," +
                        e.getCategory() + "," +
                        e.getDescription());
                bw.newLine();
            }
        }
    }
}
