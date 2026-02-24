
package com.seating.service;

import com.seating.model.Student;
import com.seating.model.Exam;
import com.seating.util.StudentReader;
import com.seating.util.ExamReader;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;

@Service
public class SeatingService {
    public List<String> generateSeatingPlan(File studentFile, File examFile, File classFile) {
        List<Student> students = StudentReader.readStudentsFromCSV(studentFile.getPath());
        List<Exam> exams = ExamReader.readExamsFromCSV(examFile.getPath());
        List<com.seating.model.Room> rooms = com.seating.util.ClassReader.readRoomsFromCSV(classFile.getPath());

        List<String> result = new ArrayList<>();

        // Group exams by date (preserving insertion order)
        Map<String, List<Exam>> examsByDate = new LinkedHashMap<>();
        for (Exam e : exams) {
            examsByDate.computeIfAbsent(e.getExamDate().trim(), k -> new ArrayList<>()).add(e);
        }

        // Process each exam date independently; rooms are available for each date
        for (Map.Entry<String, List<Exam>> dateEntry : examsByDate.entrySet()) {
            String date = dateEntry.getKey();
            List<Exam> dateExams = dateEntry.getValue();

            // Map (dept, year) → subject code for this date
            Map<String, String> deptYearToSubjCode = new LinkedHashMap<>();
            for (Exam e : dateExams) {
                String key = e.getDepartment().trim() + "_" + e.getYear().trim();
                deptYearToSubjCode.put(key, e.getSubjectCode().trim());
            }

            // Group students by subject code (subject code has priority)
            Map<String, List<Student>> studentsBySubjCode = new LinkedHashMap<>();
            for (Student s : students) {
                String key = s.getDepartment().trim() + "_" + s.getYear().trim();
                String subjCode = deptYearToSubjCode.get(key);
                if (subjCode != null) {
                    studentsBySubjCode.computeIfAbsent(subjCode, k -> new ArrayList<>()).add(s);
                }
            }

            if (studentsBySubjCode.isEmpty()) continue;

            // Sort each subject-code group by roll number
            for (List<Student> group : studentsBySubjCode.values()) {
                group.sort(Comparator.comparing(Student::getRollNo));
            }

            int numDistinctSubjects = studentsBySubjCode.size();

            if (numDistinctSubjects >= 2) {
                // ==== NORMAL CASE: different subject codes → alternating columns ====
                // (original logic preserved, queues are now subject-code groups)
                List<List<Student>> allQueues = new ArrayList<>(studentsBySubjCode.values());
                int pairIdx = 0;
                int roomIdx = 0;
                int queueGlobalIdx = 0;
                while (pairIdx < allQueues.size() && roomIdx < rooms.size()) {
                    // Pick two subject-code queues
                    List<List<Student>> queues = new ArrayList<>();
                    int[] queueIdx = new int[2];
                    for (int i = 0; i < 2 && pairIdx + i < allQueues.size(); i++) {
                        queues.add(allQueues.get(pairIdx + i));
                    }
                    if (queues.size() < 2) break; // Only one subject group left
                    boolean moreStudents = true;
                    while (moreStudents && roomIdx < rooms.size()) {
                        com.seating.model.Room room = rooms.get(roomIdx);
                        result.add("Room: " + room.getName() + " - " + date + " (Capacity: " + room.getCapacity() + ", Layout: " + room.getRows() + "x" + room.getCols() + ")");
                        String[][] grid = new String[room.getRows()][room.getCols()];
                        // Alternate columns between the two subject-code groups
                        for (int c = 0; c < room.getCols(); c++) {
                            int deptAssign = c % 2;
                            for (int r = 0; r < room.getRows(); r++) {
                                int seatNo = r * room.getCols() + c;
                                if (seatNo >= room.getCapacity()) {
                                    grid[r][c] = "-";
                                    continue;
                                }
                                // If current queue is exhausted, try to fill with next available queue
                                while (deptAssign < queues.size() && queueIdx[deptAssign] >= queues.get(deptAssign).size()) {
                                    if (queueGlobalIdx < allQueues.size()) {
                                        if (!queues.contains(allQueues.get(queueGlobalIdx)) && allQueues.get(queueGlobalIdx).size() > 0) {
                                            queues.set(deptAssign, allQueues.get(queueGlobalIdx));
                                            queueIdx[deptAssign] = 0;
                                        }
                                        queueGlobalIdx++;
                                    } else {
                                        break;
                                    }
                                }
                                if (deptAssign < queues.size() && queueIdx[deptAssign] < queues.get(deptAssign).size()) {
                                    Student placed = queues.get(deptAssign).get(queueIdx[deptAssign]++);
                                    grid[r][c] = placed.getRollNo();
                                } else {
                                    grid[r][c] = "-";
                                }
                            }
                        }
                        // Check if any students left in queues
                        moreStudents = false;
                        for (int i = 0; i < queues.size(); i++) {
                            if (queueIdx[i] < queues.get(i).size()) {
                                moreStudents = true;
                                break;
                            }
                        }
                        // Add grid to result
                        for (int r = 0; r < room.getRows(); r++) {
                            StringBuilder row = new StringBuilder();
                            for (int c = 0; c < room.getCols(); c++) {
                                row.append(grid[r][c]);
                                if (c < room.getCols() - 1) row.append(" | ");
                            }
                            result.add(row.toString());
                        }
                        result.add("");
                        roomIdx++;
                    }
                    pairIdx += 2;
                }
                // Handle remaining single subject-code queue with S|G|S gap pattern
                while (queueGlobalIdx < allQueues.size() && roomIdx < rooms.size()) {
                    List<Student> queue = allQueues.get(queueGlobalIdx);
                    int idx = 0;
                    while (idx < queue.size() && roomIdx < rooms.size()) {
                        com.seating.model.Room room = rooms.get(roomIdx);
                        result.add("Room: " + room.getName() + " - " + date + " (Capacity: " + room.getCapacity() + ", Layout: " + room.getRows() + "x" + room.getCols() + ")");
                        String[][] grid = new String[room.getRows()][room.getCols()];
                        // Column-major fill with S|G|S gap pattern (gap on c % 3 == 1)
                        for (int c = 0; c < room.getCols(); c++) {
                            for (int r = 0; r < room.getRows(); r++) {
                                if (c % 3 == 1) {
                                    grid[r][c] = "-"; // Gap column (middle of each table)
                                } else if (idx < queue.size()) {
                                    grid[r][c] = queue.get(idx++).getRollNo();
                                } else {
                                    grid[r][c] = "-";
                                }
                            }
                        }
                        for (int r = 0; r < room.getRows(); r++) {
                            StringBuilder row = new StringBuilder();
                            for (int c = 0; c < room.getCols(); c++) {
                                row.append(grid[r][c]);
                                if (c < room.getCols() - 1) row.append(" | ");
                            }
                            result.add(row.toString());
                        }
                        result.add("");
                        roomIdx++;
                    }
                    queueGlobalIdx++;
                }

            } else {
                // ==== GAP CASE: single subject code → S|G|S S|G|S S|G|S pattern ====
                // All students write the same exam (same subject code across depts, or single dept)
                // Gap on columns 1, 4, 7 (c % 3 == 1 = middle of each 3-seat table)
                // Column-major fill: top-to-bottom within each student column
                List<Student> allStudents = studentsBySubjCode.values().iterator().next();
                int idx = 0;
                for (int roomIdx = 0; roomIdx < rooms.size() && idx < allStudents.size(); roomIdx++) {
                    com.seating.model.Room room = rooms.get(roomIdx);
                    result.add("Room: " + room.getName() + " - " + date + " (Capacity: " + room.getCapacity() + ", Layout: " + room.getRows() + "x" + room.getCols() + ")");
                    String[][] grid = new String[room.getRows()][room.getCols()];
                    for (int c = 0; c < room.getCols(); c++) {
                        for (int r = 0; r < room.getRows(); r++) {
                            if (c % 3 == 1) {
                                grid[r][c] = "-"; // Gap column (middle of each table)
                            } else if (idx < allStudents.size()) {
                                grid[r][c] = allStudents.get(idx++).getRollNo();
                            } else {
                                grid[r][c] = "-";
                            }
                        }
                    }
                    for (int r = 0; r < room.getRows(); r++) {
                        StringBuilder row = new StringBuilder();
                        for (int c = 0; c < room.getCols(); c++) {
                            row.append(grid[r][c]);
                            if (c < room.getCols() - 1) row.append(" | ");
                        }
                        result.add(row.toString());
                    }
                    result.add("");
                }
            }
        }

        return result;
    }
}
