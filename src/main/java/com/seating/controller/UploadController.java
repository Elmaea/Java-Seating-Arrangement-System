package com.seating.controller;

import com.seating.service.CsvValidationService;
import com.seating.service.ValidationResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UploadController {

    private final CsvValidationService csvValidationService;

    // Use absolute path to your project's uploads directory
    private final String UPLOAD_DIR;

    @Autowired
    public UploadController(CsvValidationService csvValidationService) {
        this.csvValidationService = csvValidationService;
        // Get the absolute path to your project directory
        String projectPath = System.getProperty("user.dir");
        this.UPLOAD_DIR = projectPath + File.separator + "uploads" + File.separator;
        System.out.println("Upload directory: " + UPLOAD_DIR);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String,String>> uploadFiles(
            @RequestParam("examFile") MultipartFile examFile,
            @RequestParam("studentFile") MultipartFile studentFile,
            @RequestParam(value = "classFile", required = false) MultipartFile classFile) {
        try {
            // Create uploads directory if it doesn't exist
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                System.out.println("Created directory: " + uploadPath.toAbsolutePath());
            }

            // Validate each file's format and contents
            List<String> allErrors = new ArrayList<>();
            
            // Validate Exam file
            ValidationResult examValidation = csvValidationService.validate(examFile, "exam");
            if (!examValidation.isValid()) {
                allErrors.add("Exam file errors:");
                allErrors.addAll(examValidation.getErrors().stream()
                    .map(error -> "  - " + error)
                    .collect(Collectors.toList()));
            }

            // Validate Student file
            ValidationResult studentValidation = csvValidationService.validate(studentFile, "student");
            if (!studentValidation.isValid()) {
                allErrors.add("Student file errors:");
                allErrors.addAll(studentValidation.getErrors().stream()
                    .map(error -> "  - " + error)
                    .collect(Collectors.toList()));
            }

            // Validate Class file if provided
            if (classFile != null && !classFile.isEmpty()) {
                ValidationResult classValidation = csvValidationService.validate(classFile, "class");
                if (!classValidation.isValid()) {
                    allErrors.add("Class file errors:");
                    allErrors.addAll(classValidation.getErrors().stream()
                        .map(error -> "  - " + error)
                        .collect(Collectors.toList()));
                }
            }

            // If there are any validation errors, return them
            if (!allErrors.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", String.join("\n", allErrors));
                return ResponseEntity.badRequest().body(response);
            }

            // If all validations pass, proceed with file saving

            // Get file paths
            Path examPath = uploadPath.resolve("Exam.csv");
            Path studentPath = uploadPath.resolve("Student.csv");
            Path classPath = uploadPath.resolve("Class.csv");

            // Delete existing files if they exist
            Files.deleteIfExists(examPath);
            Files.deleteIfExists(studentPath);
            Files.deleteIfExists(classPath);

            // Save files
            examFile.transferTo(examPath.toFile());
            studentFile.transferTo(studentPath.toFile());
            if (classFile != null && !classFile.isEmpty()) {
                classFile.transferTo(classPath.toFile());
                System.out.println("Class file: " + classPath.toAbsolutePath());
            }

            System.out.println("Files successfully saved:");
            System.out.println("Exam file: " + examPath.toAbsolutePath());
            System.out.println("Student file: " + studentPath.toAbsolutePath());

            Map<String,String> res = new HashMap<>();
            res.put("message", "Files uploaded successfully");
            return ResponseEntity.ok(res);

        } catch (Exception e) {
            System.err.println("Upload error: " + e.getMessage());
            e.printStackTrace();
            Map<String,String> res = new HashMap<>();
            res.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    @PostMapping("/dept/upload")
        public ResponseEntity<Map<String,String>> deptUpload(
            jakarta.servlet.http.HttpSession session,
            @RequestParam("studentFile") MultipartFile studentFile) {
        Map<String,String> res = new HashMap<>();
        try {
            // validate
            ValidationResult studentValidation = csvValidationService.validate(studentFile, "student");
            if (!studentValidation.isValid()) {
                res.put("error", String.join("\n", studentValidation.getErrors()));
                return ResponseEntity.badRequest().body(res);
            }

            String deptName = (String) session.getAttribute("deptName");
            if (deptName == null) {
                res.put("error", "Not authenticated as a department");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) Files.createDirectories(uploadPath);

            // Save department copy
            Path deptPath = uploadPath.resolve("Student_" + deptName + ".csv");
            studentFile.transferTo(deptPath.toFile());

            // Append rows (excluding header) to master Student.csv
            Path master = uploadPath.resolve("Student.csv");
            boolean masterExists = Files.exists(master);
            try (BufferedReader r = new BufferedReader(new InputStreamReader(studentFile.getInputStream()));
                 BufferedWriter w = Files.newBufferedWriter(master, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND)) {
                String header = r.readLine();
                String line;
                // If master didn't exist, write header first
                if (!masterExists && header != null) {
                    w.write(header);
                    w.newLine();
                }
                while ((line = r.readLine()) != null) {
                    if (line.trim().isEmpty()) continue;
                    w.write(line);
                    w.newLine();
                }
            }

            res.put("message", "Department file uploaded and merged into master Student.csv");
            return ResponseEntity.ok(res);
        } catch (Exception e) {
            e.printStackTrace();
            res.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }

    // Add a test endpoint to check the upload directory
    @GetMapping("/test-upload-dir")
    public Map<String, String> testUploadDir() {
        Map<String, String> response = new HashMap<>();
        response.put("uploadDirectory", UPLOAD_DIR);
        response.put("directoryExists", String.valueOf(Files.exists(Paths.get(UPLOAD_DIR))));
        
        File uploadDir = new File(UPLOAD_DIR);
        response.put("absolutePath", uploadDir.getAbsolutePath());
        response.put("canWrite", String.valueOf(uploadDir.canWrite()));
        
        return response;
    }

    private boolean validateCsvHeader(MultipartFile file, String[] expectedHeaders) {
        if (file.isEmpty()) {
            return true; // Or false, depending on whether empty files are allowed
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return false; // File is empty or not readable
            }

            // Remove BOM if present (often added by Excel)
            if (headerLine.startsWith("\uFEFF")) {
                headerLine = headerLine.substring(1);
            }

            String[] headers = headerLine.trim().split(",");
            if (headers.length != expectedHeaders.length) {
                return false; // Different number of columns
            }

            for (int i = 0; i < expectedHeaders.length; i++) {
                if (!headers[i].trim().equalsIgnoreCase(expectedHeaders[i].trim())) {
                    return false; // Header names don't match
                }
            }
            return true; // Headers match
        } catch (IOException e) {
            System.err.println("Error reading file for validation: " + e.getMessage());
            return false;
        }
    }
}