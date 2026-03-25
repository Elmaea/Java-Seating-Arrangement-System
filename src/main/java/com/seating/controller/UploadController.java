package com.seating.controller;

import com.seating.model.Exam;
import com.seating.model.Room;
import com.seating.service.CsvValidationService;
import com.seating.service.SeatingService;
import com.seating.service.StudentService;
import com.seating.service.ValidationResult;
import com.seating.util.ClassReader;
import com.seating.util.ExamReader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UploadController {

    private final CsvValidationService csvValidationService;
    private final StudentService studentService;
    private final SeatingService seatingService;

    // Use absolute path to your project's uploads directory
    private final String UPLOAD_DIR;

    @Autowired
    public UploadController(CsvValidationService csvValidationService, StudentService studentService, SeatingService seatingService) {
        this.csvValidationService = csvValidationService;
        this.studentService = studentService;
        this.seatingService = seatingService;
        // Get the absolute path to your project directory
        String projectPath = System.getProperty("user.dir");
        this.UPLOAD_DIR = projectPath + File.separator + "uploads" + File.separator;
        System.out.println("Upload directory: " + UPLOAD_DIR);
    }

    @PostMapping("/upload")
    public ResponseEntity<Map<String,String>> uploadFiles(
            @RequestParam("examFile") MultipartFile examFile,
            @RequestParam("classFile") MultipartFile classFile) {
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

            // Validate Class file
            ValidationResult classValidation = csvValidationService.validate(classFile, "class");
            if (!classValidation.isValid()) {
                allErrors.add("Class file errors:");
                allErrors.addAll(classValidation.getErrors().stream()
                    .map(error -> "  - " + error)
                    .collect(Collectors.toList()));
            }

            // If there are any validation errors, return them
            if (!allErrors.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", String.join("\n", allErrors));
                return ResponseEntity.badRequest().body(response);
            }

            // Check if departments in exam CSV have uploaded students
            Set<String> examDepartments = extractDepartmentsFromExamCsv(examFile);
            Set<String> uploadedDepartments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            uploadedDepartments.addAll(studentService.getUploadedDepartments());

            List<String> missingDepartments = examDepartments.stream()
                    .filter(dept -> !uploadedDepartments.contains(dept))
                    .collect(Collectors.toList());

            if (!missingDepartments.isEmpty()) {
                Map<String, String> response = new HashMap<>();
                response.put("error", "Missing student uploads for department(s): " + String.join(", ", missingDepartments));
                return ResponseEntity.badRequest().body(response);
            }

            // Validate seating sufficiency using actual seating logic before saving files
            Path tempExamPath = Files.createTempFile("exam_preview_", ".csv");
            Path tempClassPath = Files.createTempFile("class_preview_", ".csv");
            try {
                Files.copy(examFile.getInputStream(), tempExamPath, StandardCopyOption.REPLACE_EXISTING);
                Files.copy(classFile.getInputStream(), tempClassPath, StandardCopyOption.REPLACE_EXISTING);

                List<com.seating.model.Student> modelStudents = studentService.getAllStudents().stream()
                        .map(s -> new com.seating.model.Student(s.getRollNo(), s.getDept(), s.getYear()))
                        .collect(Collectors.toList());

                seatingService.generateSeatingPlan(modelStudents, tempExamPath.toFile(), tempClassPath.toFile());
            } catch (IllegalStateException e) {
                Map<String, String> response = new HashMap<>();
                response.put("error", e.getMessage());
                return ResponseEntity.badRequest().body(response);
            } finally {
                Files.deleteIfExists(tempExamPath);
                Files.deleteIfExists(tempClassPath);
            }

            // If all validations pass, proceed with file saving

            // Get file paths
            Path examPath = uploadPath.resolve("Exam.csv");
            Path classPath = uploadPath.resolve("Class.csv");

            // Delete existing files if they exist
            Files.deleteIfExists(examPath);
            Files.deleteIfExists(classPath);

            // Save files
            examFile.transferTo(examPath.toFile());
            classFile.transferTo(classPath.toFile());
            System.out.println("Class file: " + classPath.toAbsolutePath());

            System.out.println("Files successfully saved:");
            System.out.println("Exam file: " + examPath.toAbsolutePath());
            System.out.println("Class file: " + classPath.toAbsolutePath());

            Map<String,String> res = new HashMap<>();
            res.put("message", "Files uploaded successfully");
            return ResponseEntity.ok(res);

        } catch (IllegalStateException e) {
            Map<String,String> res = new HashMap<>();
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
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
            // Validate CSV format and structure
            ValidationResult studentValidation = csvValidationService.validate(studentFile, "student");
            if (!studentValidation.isValid()) {
                res.put("error", String.join("\n", studentValidation.getErrors()));
                return ResponseEntity.badRequest().body(res);
            }

            // Get department name from session
            String deptName = (String) session.getAttribute("deptName");
            if (deptName == null) {
                res.put("error", "Not authenticated as a department");
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(res);
            }

            // Upload and save students to database
            // This will delete old records for this department and insert new ones
            int recordsSaved = studentService.uploadAndSaveStudents(studentFile, deptName);

            // Optionally save a backup copy of the department's CSV file
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            
            Path deptPath = uploadPath.resolve("Student_" + deptName + ".csv");
            studentFile.transferTo(deptPath.toFile());
            
            System.out.println("Department '" + deptName + "' uploaded " + recordsSaved + " student records");
            
            res.put("message", "Successfully uploaded " + recordsSaved + " student records for " + deptName);
            return ResponseEntity.ok(res);
        } catch (IOException e) {
            System.err.println("IO Error during department upload: " + e.getMessage());
            e.printStackTrace();
            res.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        } catch (IllegalArgumentException e) {
            System.err.println("Validation Error during department upload: " + e.getMessage());
            res.put("error", e.getMessage());
            return ResponseEntity.badRequest().body(res);
        } catch (Exception e) {
            System.err.println("Unexpected error during department upload: " + e.getMessage());
            e.printStackTrace();
            res.put("error", "Upload failed: " + e.getMessage());
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

    @GetMapping("/admin/student-details")
    public ResponseEntity<Map<String, Object>> getAdminStudentDetails() {
        Map<String, Object> response = new HashMap<>();
        try {
            Path uploadPath = Paths.get(UPLOAD_DIR);
            List<String> uploadedStudentFiles = new ArrayList<>();

            if (Files.exists(uploadPath)) {
                try (var stream = Files.list(uploadPath)) {
                    uploadedStudentFiles = stream
                            .map(Path::getFileName)
                            .map(Path::toString)
                            .filter(name -> name.startsWith("Student_") && name.endsWith(".csv"))
                            .sorted(String.CASE_INSENSITIVE_ORDER)
                            .collect(Collectors.toList());
                }
            }

            List<com.seating.entity.Student> students = studentService.getAllStudents();
            List<String> uploadedDepartments = studentService.getUploadedDepartments();

            File examFile = new File(UPLOAD_DIR + "Exam.csv");
            File classFile = new File(UPLOAD_DIR + "Class.csv");

            int requiredStudents = 0;
            int totalRoomCapacity = 0;
            int additionalSeatsNeeded = 0;
            boolean hasExamFile = examFile.exists();
            boolean hasClassFile = classFile.exists();

            if (hasExamFile) {
                List<Exam> exams = ExamReader.readExamsFromCSV(examFile.getAbsolutePath());
                Set<String> examDeptYearKeys = new LinkedHashSet<>();
                for (Exam exam : exams) {
                    examDeptYearKeys.add((exam.getDepartment() + "_" + exam.getYear()).trim().toUpperCase());
                }

                requiredStudents = (int) students.stream()
                        .filter(s -> examDeptYearKeys.contains((s.getDept() + "_" + s.getYear()).trim().toUpperCase()))
                        .count();
            }

            if (hasClassFile) {
                List<Room> rooms = ClassReader.readRoomsFromCSV(classFile.getAbsolutePath());
                totalRoomCapacity = rooms.stream().mapToInt(Room::getCapacity).sum();
            }

            additionalSeatsNeeded = Math.max(0, requiredStudents - totalRoomCapacity);

            response.put("uploadedStudentFiles", uploadedStudentFiles);
            response.put("uploadedDepartments", uploadedDepartments);
            response.put("totalUploadedStudents", students.size());
            response.put("requiredStudentsForSeating", requiredStudents);
            response.put("totalRoomCapacity", totalRoomCapacity);
            response.put("additionalSeatsNeeded", additionalSeatsNeeded);
            response.put("enoughCapacity", additionalSeatsNeeded == 0 && hasExamFile && hasClassFile);
            response.put("examFileUploaded", hasExamFile);
            response.put("classFileUploaded", hasClassFile);

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to load student details: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

    @GetMapping("/admin/student-file")
    public ResponseEntity<Map<String, Object>> getStudentFileContent(@RequestParam("fileName") String fileName) {
        Map<String, Object> response = new HashMap<>();

        try {
            if (fileName == null || fileName.isBlank()) {
                response.put("error", "File name is required");
                return ResponseEntity.badRequest().body(response);
            }

            // Allow only department student backup files
            if (!fileName.startsWith("Student_") || !fileName.endsWith(".csv") || fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
                response.put("error", "Invalid file name");
                return ResponseEntity.badRequest().body(response);
            }

            Path uploadPath = Paths.get(UPLOAD_DIR);
            Path filePath = uploadPath.resolve(fileName).normalize();

            if (!filePath.startsWith(uploadPath) || !Files.exists(filePath) || !Files.isRegularFile(filePath)) {
                response.put("error", "File not found");
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
            }

            List<String> lines = Files.readAllLines(filePath);
            List<String> headers = new ArrayList<>();
            List<List<String>> rows = new ArrayList<>();

            if (!lines.isEmpty()) {
                headers = parseCsvLine(lines.get(0));
                for (int i = 1; i < lines.size(); i++) {
                    String line = lines.get(i);
                    if (line == null || line.trim().isEmpty()) {
                        continue;
                    }
                    rows.add(parseCsvLine(line));
                }
            }

            response.put("fileName", fileName);
            response.put("headers", headers);
            response.put("rows", rows);
            response.put("rowCount", rows.size());
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("error", "Failed to read file: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
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

    private Set<String> extractDepartmentsFromExamCsv(MultipartFile examFile) throws IOException {
        Set<String> departments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(examFile.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            while ((line = reader.readLine()) != null) {
                if (isFirstLine) {
                    isFirstLine = false;
                    continue;
                }

                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] columns = line.split(",", -1);
                if (columns.length >= 4) {
                    String dept = columns[3].trim();
                    if (!dept.isEmpty()) {
                        departments.add(dept);
                    }
                }
            }
        }
        return departments;
    }

    private List<String> parseCsvLine(String line) {
        String[] columns = line.split(",", -1);
        List<String> result = new ArrayList<>();
        for (String column : columns) {
            result.add(column.trim());
        }
        return result;
    }
}