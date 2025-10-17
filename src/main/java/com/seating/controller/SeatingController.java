package com.seating.controller;

import com.seating.service.SeatingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import java.io.*;
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

    @GetMapping("/export-excel")
    public ResponseEntity<byte[]> exportToExcel() {
        try {
            File studentFile = new File(UPLOAD_DIR + "Student.csv");
            File examFile = new File(UPLOAD_DIR + "Exam.csv");
            File classFile = new File(UPLOAD_DIR + "Class.csv");

            if (!studentFile.exists() || !examFile.exists() || !classFile.exists()) {
                return ResponseEntity.badRequest().body("Error: Files not found".getBytes());
            }

            List<String> seatingData = seatingService.generateSeatingPlan(studentFile, examFile, classFile);
            
            try (Workbook workbook = new XSSFWorkbook()) {
                String currentRoom = null;
                Sheet currentSheet = null;
                
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
                        currentSheet = workbook.createSheet(currentRoom);
                        rowIndex = 0;
                        isHeaderRow = true;
                        
                        // Add date row at the top
                        Row dateRow = currentSheet.createRow(rowIndex++);
                        Cell dateCell = dateRow.createCell(0);
                        dateCell.setCellValue("Date: 07-09-2024");
                        
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
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }
}