
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
    public List<String> generateSeatingPlan(File studentFile, File examFile, File classFile) {
        List<Student> students = StudentReader.readStudentsFromCSV(studentFile.getPath());
        List<Exam> exams = ExamReader.readExamsFromCSV(examFile.getPath());
        List<com.seating.model.Room> rooms = com.seating.util.ClassReader.readRoomsFromCSV(classFile.getPath());

        List<String> result = new ArrayList<>();

        // Group all students by department and subject code
        java.util.Map<String, java.util.Map<String, List<Student>>> deptSubjMap = new java.util.LinkedHashMap<>();
        for (Student s : students) {
            deptSubjMap.computeIfAbsent(s.getDepartment(), k -> new java.util.LinkedHashMap<>())
                .computeIfAbsent(s.getYear(), k -> new ArrayList<>()).add(s);
        }
        // Flatten department/subject pairs
        List<String[]> deptSubjPairs = new ArrayList<>();
        for (String dept : deptSubjMap.keySet()) {
            for (String subj : deptSubjMap.get(dept).keySet()) {
                deptSubjPairs.add(new String[]{dept, subj});
            }
        }
        int pairIdx = 0;
        int roomIdx = 0;
        List<List<Student>> allQueues = new ArrayList<>();
        for (String[] pair : deptSubjPairs) {
            List<Student> list = deptSubjMap.get(pair[0]).get(pair[1]);
            list.sort((a, b) -> a.getRollNo().compareTo(b.getRollNo()));
            allQueues.add(list);
        }
        int queueGlobalIdx = 0;
        while (pairIdx < deptSubjPairs.size() && roomIdx < rooms.size()) {
            // Pick two department/subject queues for this room
            List<List<Student>> queues = new ArrayList<>();
            int[] queueIdx = new int[2];
            for (int i = 0; i < 2 && pairIdx + i < allQueues.size(); i++) {
                queues.add(allQueues.get(pairIdx + i));
            }
            if (queues.size() < 2) break; // Only one department left, stop
            boolean moreStudents = true;
            while (moreStudents && roomIdx < rooms.size()) {
                com.seating.model.Room room = rooms.get(roomIdx);
                result.add("Room: " + room.getName() + " (Capacity: " + room.getCapacity() + ", Layout: " + room.getRows() + "x" + room.getCols() + ")");
                String[][] grid = new String[room.getRows()][room.getCols()];
                // Alternate columns between the two departments, but if one runs out, fill with next available
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
                            // Find next available queue
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
        // If all queues exhausted, use spacing rule for any remaining students
        while (queueGlobalIdx < allQueues.size() && roomIdx < rooms.size()) {
            List<Student> queue = allQueues.get(queueGlobalIdx);
            int idx = 0;
            while (idx < queue.size() && roomIdx < rooms.size()) {
                com.seating.model.Room room = rooms.get(roomIdx);
                result.add("Room: " + room.getName() + " (Capacity: " + room.getCapacity() + ", Layout: " + room.getRows() + "x" + room.getCols() + ")");
                String[][] grid = new String[room.getRows()][room.getCols()];
                for (int r = 0; r < room.getRows(); r++) {
                    for (int c = 0; c < room.getCols(); c++) {
                        int seatNo = r * room.getCols() + c;
                        if (seatNo >= room.getCapacity()) {
                            grid[r][c] = "-";
                            continue;
                        }
                        if (c % 2 == 0 && idx < queue.size()) {
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
        return result;
    }
}
