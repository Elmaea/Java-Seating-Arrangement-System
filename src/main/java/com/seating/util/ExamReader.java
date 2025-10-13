package com.seating.util;

import com.seating.model.Exam;
import java.io.*;
import java.util.ArrayList;

public class ExamReader {
    public static ArrayList<Exam> readExamsFromCSV(String filePath) {
        ArrayList<Exam> exams = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;
            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }
                String[] values = line.split(",");
                if (values.length >= 5) {
                    exams.add(new Exam(values[0].trim(), values[1].trim(), values[2].trim(), 
                                     values[3].trim(), values[4].trim()));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading exam CSV: " + e.getMessage());
            e.printStackTrace();
        }
        return exams;
    }
}