package com.seating.controller;

import com.seating.service.SeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;

@RestController
@RequestMapping("/api")
public class SeatingController {

    @Autowired
    private SeatingService seatingService;

    @GetMapping("/seating")
    public List<String> getSeatingPlan() {
        File studentFile = new File("uploads/Student.csv");
        File examFile = new File("uploads/Exam.csv");
        return seatingService.generateSeatingPlan(studentFile, examFile);
    }
}
