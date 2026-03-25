package com.seating.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

@Component
public class DBConnection {

    @Value("${spring.datasource.url}")
    private String url;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url, username, password);
    }

    @PostConstruct
    public void initializeTables() {
        dropStudentTableTriggers();
        createAdminTable();
        createDeptTable();
        createStudentTable();
    }

    private void dropStudentTableTriggers() {
        String findTriggersSql = "SELECT TRIGGER_NAME FROM information_schema.TRIGGERS " +
                "WHERE TRIGGER_SCHEMA = DATABASE() AND EVENT_OBJECT_TABLE = 'student'";

        try (Connection connection = getConnection();
             PreparedStatement findStmt = connection.prepareStatement(findTriggersSql);
             ResultSet rs = findStmt.executeQuery()) {

            while (rs.next()) {
                String triggerName = rs.getString("TRIGGER_NAME");
                if (triggerName == null || triggerName.isBlank()) {
                    continue;
                }

                try (Statement dropStmt = connection.createStatement()) {
                    dropStmt.execute("DROP TRIGGER IF EXISTS `" + triggerName + "`");
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to remove legacy triggers from student table", e);
        }
    }

    private void createAdminTable() {
        String sql = "CREATE TABLE IF NOT EXISTS admin (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL" +
                ")";
        executeDDL(sql, "admin");
    }

    private void createDeptTable() {
        String sql = "CREATE TABLE IF NOT EXISTS dept (" +
                "id BIGINT PRIMARY KEY AUTO_INCREMENT, " +
                "department_name VARCHAR(100) NOT NULL, " +
                "email VARCHAR(255) NOT NULL UNIQUE, " +
                "password VARCHAR(255) NOT NULL" +
                ")";
        executeDDL(sql, "dept");
    }

    private void createStudentTable() {
        String sql = "CREATE TABLE IF NOT EXISTS student (" +
                "Rollno VARCHAR(20) PRIMARY KEY, " +
                "Dept VARCHAR(20), " +
                "Year VARCHAR(10)" +
                ")";
        executeDDL(sql, "student");
    }

    private void executeDDL(String sql, String tableName) {
        try (Connection connection = getConnection();
             Statement statement = connection.createStatement()) {
            statement.execute(sql);
        } catch (SQLException e) {
            throw new RuntimeException("Failed to initialize table: " + tableName, e);
        }
    }
}
