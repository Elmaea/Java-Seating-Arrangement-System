package com.seating.repository;

import com.seating.config.DBConnection;
import com.seating.entity.Student;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.ArrayList;
import java.util.TreeSet;

@Component
public class StudentRepository {

    private final DBConnection dbConnection;

    public StudentRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public List<Student> findByDept(String dept) {
        String sql = "SELECT Rollno, Dept, Year FROM student WHERE Dept = ?";
        List<Student> students = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dept);
            try (ResultSet rs = statement.executeQuery()) {
                while (rs.next()) {
                    Student student = new Student();
                    student.setRollNo(rs.getString("Rollno"));
                    student.setDept(rs.getString("Dept"));
                    student.setYear(rs.getString("Year"));
                    students.add(student);
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch students by department", e);
        }

        return students;
    }

    public void deleteByDept(String dept) {
        String sql = "DELETE FROM student WHERE Dept = ?";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, dept);
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete students by department", e);
        }
    }

    public void deleteAll() {
        String sql = "DELETE FROM student";
        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to delete all students", e);
        }
    }

    public List<Student> findAll() {
        String sql = "SELECT Rollno, Dept, Year FROM student";
        List<Student> students = new ArrayList<>();

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                Student student = new Student();
                student.setRollNo(rs.getString("Rollno"));
                student.setDept(rs.getString("Dept"));
                student.setYear(rs.getString("Year"));
                students.add(student);
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch all students", e);
        }

        return students;
    }

    public void saveAll(List<Student> students) {
        if (students == null || students.isEmpty()) {
            return;
        }

        String sql = "INSERT INTO student (Rollno, Dept, Year) VALUES (?, ?, ?) " +
                "ON DUPLICATE KEY UPDATE Dept = VALUES(Dept), Year = VALUES(Year)";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            for (Student student : students) {
                statement.setString(1, student.getRollNo());
                statement.setString(2, student.getDept());
                statement.setString(3, student.getYear());
                statement.addBatch();
            }
            statement.executeBatch();
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save students", e);
        }
    }

    public List<String> findDistinctDepts() {
        String sql = "SELECT DISTINCT Dept FROM student WHERE Dept IS NOT NULL AND TRIM(Dept) <> ''";
        TreeSet<String> departments = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet rs = statement.executeQuery()) {
            while (rs.next()) {
                String dept = rs.getString("Dept");
                if (dept != null && !dept.trim().isEmpty()) {
                    departments.add(dept.trim());
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch uploaded departments", e);
        }

        return new ArrayList<>(departments);
    }
}
