package com.seating.repository;

import com.seating.config.DBConnection;
import com.seating.entity.Admin;
import org.springframework.stereotype.Component;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

@Component
public class AdminRepository {

    private final DBConnection dbConnection;

    public AdminRepository(DBConnection dbConnection) {
        this.dbConnection = dbConnection;
    }

    public Admin findByEmail(String email) {
        String sql = "SELECT id, email, password FROM admin WHERE email = ?";

        try (Connection connection = dbConnection.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, email);
            try (ResultSet rs = statement.executeQuery()) {
                if (rs.next()) {
                    Admin admin = new Admin();
                    admin.setId(rs.getLong("id"));
                    admin.setEmail(rs.getString("email"));
                    admin.setPassword(rs.getString("password"));
                    return admin;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Failed to fetch admin by email", e);
        }

        return null;
    }
}
