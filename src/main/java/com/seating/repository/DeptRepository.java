package com.seating.repository;

import com.seating.config.DBConnection;
import com.seating.entity.Dept;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DeptRepository {

    private final DBConnection dbConnection;

    public DeptRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public Dept findByEmail(String email) {
        String sql = "SELECT id, department_name, email, password FROM dept WHERE email = ?";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Dept dept = new Dept();
                    dept.setId(rs.getLong("id"));
                    dept.setDeptName(rs.getString("department_name"));
                    dept.setEmail(rs.getString("email"));
                    dept.setPassword(rs.getString("password"));
                    return dept;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch department by email", e);
        }

        return null;
    }

    public Dept save(Dept dept) {
        String sql = "INSERT INTO dept (department_name, email, password) VALUES (?, ?, ?)";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, dept.getDeptName());
            statement.setString(2, dept.getEmail());
            statement.setString(3, dept.getPassword());
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    dept.setId(keys.getLong(1));
                }
            }
            return dept;
        } catch (SQLException e) {
            throw new RuntimeException("Failed to save department", e);
        }
    }
}
