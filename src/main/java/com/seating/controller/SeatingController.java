package com.seating.controller;

import com.seating.service.SeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SeatingController {

    @Autowired
    private SeatingService seatingService;

    private final String UPLOAD_DIR;

    public SeatingController() {
        String projectPath = System.getProperty("user.dir");
        this.UPLOAD_DIR = projectPath + File.separator + "uploads" + File.separator;
    }

    @GetMapping("/seating")
    public List<String> getSeatingPlan() {
        File studentFile = new File(UPLOAD_DIR + "Student.csv");
        File examFile = new File(UPLOAD_DIR + "Exam.csv");
        File classFile = new File(UPLOAD_DIR + "Class.csv");

        System.out.println("Looking for files in: " + UPLOAD_DIR);
        System.out.println("Student file exists: " + studentFile.exists() + " at " + studentFile.getAbsolutePath());
        System.out.println("Exam file exists: " + examFile.exists() + " at " + examFile.getAbsolutePath());
        System.out.println("Class file exists: " + classFile.exists() + " at " + classFile.getAbsolutePath());

        // Check if files exist
        if (!studentFile.exists() || !examFile.exists() || !classFile.exists()) {
            return List.of("Error: Please upload CSV files first. Files not found in: " + UPLOAD_DIR);
        }

        return seatingService.generateSeatingPlan(studentFile, examFile, classFile);
    }
}