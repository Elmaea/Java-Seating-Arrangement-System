package com.seating.service;

import com.seating.model.Student;
import com.seating.model.Exam;
import com.seating.util.StudentReader;
import com.seating.util.ExamReader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Service
public class SeatingService {
    public List<String> generateSeatingPlan(File studentFile, File examFile) {
        List<Student> students = StudentReader.readStudentsFromCSV(studentFile.getPath());
        List<Exam> exams = ExamReader.readExamsFromCSV(examFile.getPath());

        List<String> seatingPlan = new ArrayList<>();
        for (Student s : students) {
            for (Exam e : exams) {
                if (s.getDepartment().equalsIgnoreCase(e.getDepartment()) &&
                    s.getYear().equalsIgnoreCase(e.getYear())) {
                    seatingPlan.add(s.getRollNo() + " | " + e.getSubject() + " | " + e.getExamDate());
                }
            }
        }
        return seatingPlan;
    }
}
