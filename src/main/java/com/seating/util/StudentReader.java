package com.seating.util;

import com.seating.model.Student;
import java.io.*;
import java.util.ArrayList;

public class StudentReader {

    public static ArrayList<Student> readStudentsFromCSV(String filePath) {
        ArrayList<Student> students = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                String[] values = line.split(",");
                if (values.length >= 3) {
                    students.add(new Student(
                        values[0].trim(),
                        values[1].trim(),
                        values[2].trim()
                    ));
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading student CSV: " + e.getMessage());
            e.printStackTrace();
        }

        return students;
    }
}
