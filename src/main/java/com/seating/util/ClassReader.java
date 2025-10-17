package com.seating.util;

import com.seating.model.Room;
import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class ClassReader {

    public static List<Room> readRoomsFromCSV(String filePath) {
        List<Room> rooms = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(filePath))) {
            String line;
            boolean isFirstLine = true;

            while ((line = br.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header
                }

                String[] values = line.split(",");
                if (values.length >= 4) {
                    String name = values[0].trim();
                    int capacity = Integer.parseInt(values[1].trim());
                    int rows = Integer.parseInt(values[2].trim());
                    int cols = Integer.parseInt(values[3].trim());

                    rooms.add(new Room(name, capacity, rows, cols));
                }
            }
        } catch (IOException | NumberFormatException e) {
            System.err.println("Error reading class CSV: " + e.getMessage());
            e.printStackTrace();
        }

        return rooms;
    }
}
