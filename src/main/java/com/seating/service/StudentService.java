package com.seating.service;

import com.seating.entity.Student;
import com.seating.repository.StudentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    /**
     * Process and save student data from CSV file for a specific department.
     * If students from this department already exist in the database, they will be replaced.
     * 
     * @param studentFile The CSV file containing student data
     * @param dept The department name
     * @return The number of students saved
     * @throws IOException if file reading fails
     */
    @Transactional
    public int uploadAndSaveStudents(MultipartFile studentFile, String dept) throws IOException {
        // Read students from CSV
        List<Student> students = readStudentsFromCSV(studentFile, dept);

        if (students.isEmpty()) {
            throw new IllegalArgumentException("No valid student records found in the file");
        }

        // Delete existing students for this department
        studentRepository.deleteByDept(dept);
        
        // Save new students
        studentRepository.saveAll(students);
        
        return students.size();
    }

    /**
     * Read student data from CSV file and parse into Student entities
     * Ensures all students belong to the specified department
     * 
     * @param studentFile The CSV file to read
     * @param dept The department name
     * @return List of Student entities
     * @throws IOException if file reading fails
     */
    private List<Student> readStudentsFromCSV(MultipartFile studentFile, String dept) throws IOException {
        List<Student> students = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(studentFile.getInputStream()))) {
            String line;
            boolean isFirstLine = true;
            int rowNum = 1;

            while ((line = reader.readLine()) != null) {
                rowNum++;
                
                if (isFirstLine) {
                    isFirstLine = false;
                    continue; // Skip header row
                }

                if (line.trim().isEmpty()) {
                    continue; // Skip empty lines
                }

                String[] values = line.split(",", -1);
                if (values.length < 3) {
                    System.out.println("Row " + rowNum + ": insufficient columns, skipping");
                    continue;
                }

                String rollNo = values[0].trim();
                String deptFromFile = values[1].trim();
                String year = values[2].trim();

                // Skip empty roll numbers
                if (rollNo.isEmpty()) {
                    continue;
                }

                // Validate that department in file matches the uploading department
                if (!deptFromFile.equalsIgnoreCase(dept)) {
                    System.out.println("Row " + rowNum + ": department mismatch - expected '" + dept + 
                               "' but found '" + deptFromFile + "', skipping");
                    continue;
                }

                Student student = new Student(rollNo, dept, year);
                students.add(student);
            }
        }

        return students;
    }

    /**
     * Get all students for a specific department
     * 
     * @param dept The department name
     * @return List of students in that department
     */
    public List<Student> getStudentsByDept(String dept) {
        return studentRepository.findByDept(dept);
    }

    /**
     * Get all students from the database
     * 
     * @return List of all students
     */
    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    /**
     * Delete all students for a specific department
     * 
     * @param dept The department name
     */
    @Transactional
    public void deleteStudentsByDept(String dept) {
        studentRepository.deleteByDept(dept);
    }

    /**
     * Delete all students from the database
     */
    @Transactional
    public void deleteAllStudents() {
        studentRepository.deleteAll();
    }
}
