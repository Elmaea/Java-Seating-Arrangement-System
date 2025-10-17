package com.seating.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.nio.file.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class UploadController {

    // Use absolute path to your project's uploads directory
    private final String UPLOAD_DIR;

    public UploadController() {
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

            // Validate that required files are not empty
            if (examFile.isEmpty() || studentFile.isEmpty()) {
                Map<String,String> res = new HashMap<>();
                res.put("message", "Please select both Exam.csv and Student.csv files");
                return ResponseEntity.badRequest().body(res);
            }

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
}