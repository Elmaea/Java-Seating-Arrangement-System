package com.seating.util;

import com.seating.model.Student;
import java.io.*;
import java.util.ArrayList;

public class StudentReader {
    public static ArrayList<Student> readStudentsFromCSV(String filePath) {
        ArrayList<Student> students = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] values = line.split(",");
                if (values.length == 3) {
                    students.add(new Student(values[0].trim(), values[1].trim(), values[2].trim()));
                }
            }
        } catch (IOException e) { e.printStackTrace(); }
        return students;
    }
}
