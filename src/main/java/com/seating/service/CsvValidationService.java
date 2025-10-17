package com.seating.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

@Service
public class CsvValidationService {
    private static final Map<String, String[]> EXPECTED_HEADERS = new HashMap<>();
    
    static {
        EXPECTED_HEADERS.put("student", new String[]{"Rollno", "Dept", "Year"});
        EXPECTED_HEADERS.put("exam", new String[]{"ExamDate", "Subject", "Subject Code", "Department", "Year"});
        EXPECTED_HEADERS.put("class", new String[]{"Class", "Maximum Capacity", "Number of Rows", "Number of Columns"});
    }

    private static final Map<String, Integer> FILE_COLUMN_COUNTS = new HashMap<>();
    
    static {
        FILE_COLUMN_COUNTS.put("student", 3);  // Changed from 4 to 3
        FILE_COLUMN_COUNTS.put("exam", 5);     // Kept as 5
        FILE_COLUMN_COUNTS.put("class", 4);    // Kept as 4
    }

    public ValidationResult validate(MultipartFile file, String fileType) {
        List<String> errors = new ArrayList<>();
        
        if (file.isEmpty()) {
            return new ValidationResult(false, List.of("File is empty."));
        }

        if (!FILE_COLUMN_COUNTS.containsKey(fileType)) {
            return new ValidationResult(false, List.of("Invalid file type specified."));
        }

        int expectedColumnCount = FILE_COLUMN_COUNTS.get(fileType);
        String[] expectedHeaders = EXPECTED_HEADERS.get(fileType);

        // Print debug information
        System.out.println("\n=== File Validation Debug Info ===");
        System.out.println("File type: " + fileType);
        System.out.println("File name: " + file.getOriginalFilename());
        System.out.println("Expected column count: " + expectedColumnCount);
        System.out.println("Expected headers: " + String.join(", ", expectedHeaders));

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            // Validate header
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return new ValidationResult(false, List.of("File is empty or missing headers."));
            }

            String[] headers = headerLine.split(",", -1);
            validateHeaders(headers, expectedHeaders, errors);

            // Validate data rows
            String line;
            int rowNum = 2; // Start from row 2 since row 1 is header

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1);

                if (columns.length != expectedColumnCount) {
                    errors.add(String.format("Row %d: Expected %d columns but found %d columns.", 
                        rowNum, expectedColumnCount, columns.length));
                    continue;
                }

                validateRow(columns, rowNum, fileType, errors);
                rowNum++;
            }
        } catch (IOException e) {
            errors.add("Failed to read file: " + e.getMessage());
        }

        return errors.isEmpty() ? ValidationResult.valid() : new ValidationResult(false, errors);
    }

    private void validateHeaders(String[] headers, String[] expectedHeaders, List<String> errors) {
        System.out.println("\n=== Header Validation Debug Info ===");
        System.out.println("Raw headers found: " + String.join(", ", headers));

        // Clean up headers by trimming whitespace, removing BOM and normalizing case
        String[] cleanedHeaders = Arrays.stream(headers)
            .map(h -> h.trim().replaceAll("\\r|\\n", "")) // Remove any line endings
            .map(h -> h.startsWith("\uFEFF") ? h.substring(1) : h) // Remove BOM if present
            .map(String::trim)
            .map(String::toLowerCase) // Convert to lowercase for case-insensitive comparison
            .toArray(String[]::new);

        // Convert expected headers to lowercase for comparison
        String[] normalizedExpectedHeaders = Arrays.stream(expectedHeaders)
            .map(String::toLowerCase)
            .toArray(String[]::new);

        System.out.println("Cleaned headers: " + String.join(", ", cleanedHeaders));
        System.out.println("Normalized expected headers: " + String.join(", ", normalizedExpectedHeaders));

        if (cleanedHeaders.length != normalizedExpectedHeaders.length) {
            errors.add(String.format("Invalid header count. Expected: %d, Found: %d", 
                normalizedExpectedHeaders.length, cleanedHeaders.length));
            errors.add("Expected headers: " + String.join(", ", expectedHeaders));
            errors.add("Found headers: " + String.join(", ", headers));
            return;
        }

        for (int i = 0; i < normalizedExpectedHeaders.length; i++) {
            System.out.println(String.format("Comparing header %d: Expected='%s', Found='%s'", 
                i + 1, normalizedExpectedHeaders[i], cleanedHeaders[i]));
            if (!normalizedExpectedHeaders[i].equals(cleanedHeaders[i])) {
                errors.add(String.format("Invalid header at position %d. Expected: '%s', Found: '%s'", 
                    i + 1, expectedHeaders[i], headers[i]));
            }
        }
    }

    private void validateRow(String[] columns, int rowNum, String fileType, List<String> errors) {
        System.out.println("\n=== Row " + rowNum + " Validation ===");
        System.out.println("Row content: " + String.join(", ", columns));
        
        // Check column count first
        if (columns.length != FILE_COLUMN_COUNTS.get(fileType)) {
            System.out.println(String.format("Invalid column count in row %d: Expected %d, Found %d", 
                rowNum, FILE_COLUMN_COUNTS.get(fileType), columns.length));
            errors.add(String.format("Row %d: Invalid number of columns. Expected %d, Found %d", 
                rowNum, FILE_COLUMN_COUNTS.get(fileType), columns.length));
            return;
        }

        // Check for empty values
        for (int i = 0; i < columns.length; i++) {
            String value = columns[i].trim();
            if (value.isEmpty()) {
                System.out.println(String.format("Empty value found in column %d ('%s')", 
                    i + 1, EXPECTED_HEADERS.get(fileType)[i]));
                errors.add(String.format("Row %d: Empty value in column '%s'", 
                    rowNum, EXPECTED_HEADERS.get(fileType)[i]));
                continue;
            }

            // Specific validations based on file type and column
            switch (fileType) {
                case "student":
                    validateStudentData(columns, rowNum, i, errors);
                    break;
                case "exam":
                    validateExamData(columns, rowNum, i, errors);
                    break;
                case "class":
                    validateClassData(columns, rowNum, i, errors);
                    break;
            }
        }
    }

    private void validateStudentData(String[] columns, int rowNum, int colIndex, List<String> errors) {
        String value = columns[colIndex].trim();
        switch (colIndex) {
            case 0: // Rollno - Format: B[YY][DEPT][Section][Number] (e.g., B24EEEB04)
                if (!value.matches("B\\d{2}[A-Z]{2,}[A-Z]\\d{2}")) {
                    System.out.println(String.format("Invalid Roll number: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Roll number '%s' must be in format BYYDEPTSNN", rowNum, value));
                }
                break;
            case 1: // Dept
                if (value.length() < 2) {
                    System.out.println(String.format("Invalid Department: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Department name '%s' is too short", rowNum, value));
                }
                break;
            case 2: // Year - Allow 2-digit years
                if (!value.matches("\\d{2}")) {
                    System.out.println(String.format("Invalid Year: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Year '%s' must be a two-digit number", rowNum, value));
                }
                break;
        }
    }

    private void validateExamData(String[] columns, int rowNum, int colIndex, List<String> errors) {
        String value = columns[colIndex].trim();
        switch (colIndex) {
            case 0: // ExamDate (dd-mm-yy format)
                if (!value.matches("\\d{1,2}-\\d{1,2}-\\d{2}")) {
                    System.out.println(String.format("Invalid Exam Date: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Date must be in DD-MM-YY format", rowNum));
                }
                break;
            case 1: // Subject
                if (value.isEmpty()) {
                    System.out.println(String.format("Empty Subject at row %d", rowNum));
                    errors.add(String.format("Row %d: Subject cannot be empty", rowNum));
                }
                break;
            case 2: // Subject Code (format: YYMATSNN or YYCSMMM etc)
                if (!value.matches("\\d{2}[A-Z]{2,}\\d{3}")) {
                    System.out.println(String.format("Invalid Subject Code: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Subject Code must be in format YYDEPTXXX", rowNum));
                }
                break;
            case 3: // Department
                if (value.length() < 2 || !value.matches("CSE|EC|MECH|CIVIL|EEE")) {
                    System.out.println(String.format("Invalid Department: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Invalid department code", rowNum));
                }
                break;
            case 4: // Year (23, 24 format)
                if (!value.matches("\\d{2}")) {
                    System.out.println(String.format("Invalid Year: '%s' at row %d", value, rowNum));
                    errors.add(String.format("Row %d: Year must be in YY format", rowNum));
                }
                break;
        }
    }

    private void validateClassData(String[] columns, int rowNum, int colIndex, List<String> errors) {
        switch (colIndex) {
            case 0: // Class
                if (columns[colIndex].trim().isEmpty()) {
                    errors.add(String.format("Row %d: Class name cannot be empty", rowNum));
                }
                break;
            case 1: // Maximum Capacity
            case 2: // Number of Rows
            case 3: // Number of Columns
                if (!columns[colIndex].matches("\\d+")) {
                    errors.add(String.format("Row %d: %s must be numeric", 
                        rowNum, EXPECTED_HEADERS.get("class")[colIndex]));
                }
                break;
        }
    }
}
