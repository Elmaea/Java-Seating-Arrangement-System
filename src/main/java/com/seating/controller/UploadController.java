package com.seating.controller;

import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class UploadController {

    @PostMapping("/upload")
    public ResponseEntity<Map<String,String>> uploadFiles(@RequestParam("examFile") MultipartFile examFile,
                                                          @RequestParam("studentFile") MultipartFile studentFile) {
        try {
            File exam = new File("uploads/Exam.csv");
            File student = new File("uploads/Student.csv");
            examFile.transferTo(exam);
            studentFile.transferTo(student);

            Map<String,String> res = new HashMap<>();
            res.put("message", "Files uploaded successfully");
            return ResponseEntity.ok(res);
        } catch (IOException e) {
            Map<String,String> res = new HashMap<>();
            res.put("message", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(res);
        }
    }
}
