package com.seating.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Service
public class CsvValidationService {

    public ValidationResult validate(MultipartFile file, int expectedColumnCount) {
        List<String> errors = new ArrayList<>();
        if (file.isEmpty()) {
            return new ValidationResult(false, List.of("File is empty."));
        }

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String line;
            int rowNum = 1;
            // Skip header
            reader.readLine();
            rowNum++;

            while ((line = reader.readLine()) != null) {
                String[] columns = line.split(",", -1); // Use -1 to include trailing empty strings

                if (columns.length != expectedColumnCount) {
                    errors.add(String.format("Incorrect number of columns at row %d in %s. Expected %d, found %d.", rowNum, file.getOriginalFilename(), expectedColumnCount, columns.length));
                    continue; // Move to next row
                }

                for (int i = 0; i < columns.length; i++) {
                    if (columns[i] == null || columns[i].trim().isEmpty()) {
                        errors.add(String.format("Missing data at row %d, column %d in %s.", rowNum, i + 1, file.getOriginalFilename()));
                    }
                }
                rowNum++;
            }
        } catch (Exception e) {
            errors.add("Failed to read or process the file: " + file.getOriginalFilename());
        }

        if (!errors.isEmpty()) {
            return new ValidationResult(false, errors);
        }

        return ValidationResult.valid();
    }
}
