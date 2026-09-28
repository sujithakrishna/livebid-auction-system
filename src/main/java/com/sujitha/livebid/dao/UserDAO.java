package com.sujitha.livebid.dao;

import com.sujitha.livebid.config.DatabaseConnection;
import com.sujitha.livebid.model.User;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.time.LocalDateTime;

public class UserDAO {

    public User save(User user) throws SQLException {

        String sql = """
                INSERT INTO users (name, email, password_hash)
                VALUES (?, ?, ?)
                """;

        long generatedId;

        try (Connection connection = DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setString(1, user.getName());
            statement.setString(2, user.getEmail());
            statement.setString(3, user.getPasswordHash());

            int rowsAffected = statement.executeUpdate();

            if (rowsAffected == 0) {
                throw new SQLException(
                        "User registration failed."
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {

                    generatedId =
                            generatedKeys.getLong(1);

                } else {

                    throw new SQLException(
                            "User registration failed. "
                                    + "No ID generated."
                    );
                }
            }
        }

        // Retrieve the complete user record,
        // including the database-generated created_at value.
        User savedUser = findById(generatedId);

        if (savedUser == null) {
            throw new SQLException(
                    "User was inserted but could not be retrieved."
            );
        }

        return savedUser;
    }


    public User findById(long id) throws SQLException {

        String sql = """
                SELECT
                    id,
                    name,
                    email,
                    password_hash,
                    created_at
                FROM users
                WHERE id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, id);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }


    public User findByEmail(String email) throws SQLException {

        String sql = """
                SELECT
                    id,
                    name,
                    email,
                    password_hash,
                    created_at
                FROM users
                WHERE email = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(1, email);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {
                    return mapUser(resultSet);
                }
            }
        }

        return null;
    }


    private User mapUser(
            ResultSet resultSet) throws SQLException {

        LocalDateTime createdAt = null;

        if (resultSet.getTimestamp("created_at") != null) {
            createdAt = resultSet
                    .getTimestamp("created_at")
                    .toLocalDateTime();
        }

        return new User(
                resultSet.getLong("id"),
                resultSet.getString("name"),
                resultSet.getString("email"),
                resultSet.getString("password_hash"),
                createdAt
        );
    }
}