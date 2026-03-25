package com.seating.controller;

import com.seating.service.SeatingService;
import com.seating.service.StudentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.WorkbookUtil;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
public class SeatingController {

    @Autowired
    private SeatingService seatingService;

    @Autowired
    private StudentService studentService;

    private final String UPLOAD_DIR;

    public SeatingController() {
        String projectPath = System.getProperty("user.dir");
        this.UPLOAD_DIR = projectPath + File.separator + "uploads" + File.separator;
    }

    @GetMapping("/seating")
    public List<String> getSeatingPlan() {
        File examFile = new File(UPLOAD_DIR + "Exam.csv");
        File classFile = new File(UPLOAD_DIR + "Class.csv");
        List<com.seating.entity.Student> dbStudents = studentService.getAllStudents();
        List<com.seating.model.Student> students = new ArrayList<>();
        for (com.seating.entity.Student s : dbStudents) {
            students.add(new com.seating.model.Student(s.getRollNo(), s.getDept(), s.getYear()));
        }

        System.out.println("Looking for files in: " + UPLOAD_DIR);
        System.out.println("Student records in DB: " + students.size());
        System.out.println("Exam file exists: " + examFile.exists() + " at " + examFile.getAbsolutePath());
        System.out.println("Class file exists: " + classFile.exists() + " at " + classFile.getAbsolutePath());

        // Check if files exist
        if (!examFile.exists() || !classFile.exists()) {
            return List.of("Error: Please upload Exam and Class CSV files first. Files not found in: " + UPLOAD_DIR);
        }

        if (students.isEmpty()) {
            return List.of("Error: No student data found. Departments must upload student CSV first.");
        }

        try {
            return seatingService.generateSeatingPlan(students, examFile, classFile);
        } catch (IllegalStateException e) {
            return List.of("Error: " + e.getMessage());
        }
    }

    @GetMapping("/uploaded-departments")
    public ResponseEntity<Map<String, Object>> getUploadedDepartments() {
        List<com.seating.entity.Student> dbStudents = studentService.getAllStudents();
        TreeSet<String> departments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        for (com.seating.entity.Student student : dbStudents) {
            if (student.getDept() != null && !student.getDept().trim().isEmpty()) {
                departments.add(student.getDept().trim());
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("count", departments.size());
        response.put("departments", new ArrayList<>(departments));
        return ResponseEntity.ok(response);
    }

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            File examFile = new File(UPLOAD_DIR + "Exam.csv");
            File classFile = new File(UPLOAD_DIR + "Class.csv");
            List<com.seating.entity.Student> dbStudents = studentService.getAllStudents();
            List<com.seating.model.Student> students = new ArrayList<>();
            for (com.seating.entity.Student s : dbStudents) {
                students.add(new com.seating.model.Student(s.getRollNo(), s.getDept(), s.getYear()));
            }

            if (!examFile.exists() || !classFile.exists()) {
                return ResponseEntity.badRequest().body("Error: Exam/Class files not found".getBytes());
            }

            if (students.isEmpty()) {
                return ResponseEntity.badRequest().body("Error: No student data found in database".getBytes());
            }

            List<String> seatingData = seatingService.generateSeatingPlan(students, examFile, classFile);
            
            try (Workbook workbook = new XSSFWorkbook()) {
                String currentRoom = null;
                Sheet currentSheet = null;
                Set<String> usedSheetNames = new HashSet<>();
                
                // Create cell styles
                CellStyle defaultStyle = workbook.createCellStyle();
                defaultStyle.setBorderBottom(BorderStyle.THIN);
                defaultStyle.setBorderTop(BorderStyle.THIN);
                defaultStyle.setBorderLeft(BorderStyle.THIN);
                defaultStyle.setBorderRight(BorderStyle.THIN);
                defaultStyle.setAlignment(HorizontalAlignment.CENTER);
                defaultStyle.setVerticalAlignment(VerticalAlignment.CENTER);

                CellStyle mechStyle = workbook.createCellStyle();
                mechStyle.cloneStyleFrom(defaultStyle);
                mechStyle.setFillForegroundColor(IndexedColors.BRIGHT_GREEN.getIndex());
                mechStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

                int rowIndex = 0;
                boolean isHeaderRow = true;

                for (String line : seatingData) {
                    if (line.startsWith("Room:")) {
                        // Start new sheet
                        currentRoom = line.split("\\(")[0].replace("Room:", "").trim();
                        String safeSheetName = buildUniqueSheetName(currentRoom, usedSheetNames);
                        currentSheet = workbook.createSheet(safeSheetName);
                        rowIndex = 0;
                        isHeaderRow = true;
                        
                        // Add date row at the top
                        Row dateRow = currentSheet.createRow(rowIndex++);
                        Cell dateCell = dateRow.createCell(0);
                        dateCell.setCellValue("Date: " + extractDateFromRoomLine(line));
                        
                        // Add empty row
                        currentSheet.createRow(rowIndex++);
                        
                    } else if (!line.trim().isEmpty() && currentSheet != null) {
                        String[] cells = line.split("\\|");
                        Row row = currentSheet.createRow(rowIndex++);
                        row.setHeightInPoints(30); // Set row height
                        
                        for (int i = 0; i < cells.length; i++) {
                            Cell cell = row.createCell(i);
                            String value = cells[i].trim();
                            cell.setCellValue(value);
                            
                            // Apply green style only to columns 2, 5, and 8 (index 1, 4, and 7)
                            if (i == 1 || i == 4 || i == 7) {
                                cell.setCellStyle(mechStyle);
                            } else {
                                cell.setCellStyle(defaultStyle);
                            }
                        }
                        
                        // Add empty row after each filled row
                        currentSheet.createRow(rowIndex++);
                    }
                }

                // Format all sheets
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    sheet.setDefaultColumnWidth(15);
                    
                    // Set print setup
                    PrintSetup printSetup = sheet.getPrintSetup();
                    printSetup.setLandscape(true);
                    sheet.setFitToPage(true);
                    printSetup.setFitHeight((short)1);
                    printSetup.setFitWidth((short)1);
                }

                // Convert workbook to byte array
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                workbook.write(outputStream);

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
                headers.setContentDispositionFormData("attachment", "SeatingPlan.xlsx");

                return ResponseEntity
                    .ok()
                    .headers(headers)
                    .body(outputStream.toByteArray());
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(("Error: " + e.getMessage()).getBytes());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

    private String extractDateFromRoomLine(String line) {
        try {
            int dashIndex = line.indexOf(" - ");
            int capIndex = line.indexOf(" (Capacity:");
            if (dashIndex >= 0 && capIndex > dashIndex + 3) {
                return line.substring(dashIndex + 3, capIndex).trim();
            }
        } catch (Exception ignored) {
        }
        return "N/A";
    }

    private String buildUniqueSheetName(String rawName, Set<String> usedSheetNames) {
        String safeBase = WorkbookUtil.createSafeSheetName(rawName == null ? "Sheet" : rawName.trim());
        if (safeBase == null || safeBase.isBlank()) {
            safeBase = "Sheet";
        }

        safeBase = safeBase.length() > 31 ? safeBase.substring(0, 31) : safeBase;
        if (!usedSheetNames.contains(safeBase)) {
            usedSheetNames.add(safeBase);
            return safeBase;
        }

        int suffix = 2;
        while (true) {
            String marker = "_" + suffix;
            int maxBaseLength = 31 - marker.length();
            String candidateBase = safeBase.length() > maxBaseLength ? safeBase.substring(0, maxBaseLength) : safeBase;
            String candidate = candidateBase + marker;
            if (!usedSheetNames.contains(candidate)) {
                usedSheetNames.add(candidate);
                return candidate;
            }
            suffix++;
        }
    }
}