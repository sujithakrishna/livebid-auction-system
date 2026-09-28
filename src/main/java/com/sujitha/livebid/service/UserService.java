package com.sujitha.livebid.service;

import com.sujitha.livebid.dao.UserDAO;
import com.sujitha.livebid.model.User;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

public class UserService {

    private final UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public User registerUser(String name, String email, String password)
            throws SQLException {

        validateRegistrationData(name, email, password);

        String normalizedEmail = email.trim().toLowerCase();

        User existingUser = userDAO.findByEmail(normalizedEmail);

        if (existingUser != null) {
            throw new IllegalArgumentException(
                    "An account with this email already exists.");
        }

        String passwordHash = hashPassword(password);

        User user = new User(
                name.trim(),
                normalizedEmail,
                passwordHash
        );

        return userDAO.save(user);
    }

    private void validateRegistrationData(
            String name,
            String email,
            String password) {

        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Name cannot be empty.");
        }

        if (email == null || email.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Email cannot be empty.");
        }

        if (!email.matches(
                "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {

            throw new IllegalArgumentException(
                    "Invalid email address.");
        }

        if (password == null || password.length() < 6) {
            throw new IllegalArgumentException(
                    "Password must contain at least 6 characters.");
        }
    }

    private String hashPassword(String password) {

        try {

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            password.getBytes(StandardCharsets.UTF_8));

            StringBuilder hexString = new StringBuilder();

            for (byte b : hash) {
                hexString.append(
                        String.format("%02x", b));
            }

            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {

            throw new IllegalStateException(
                    "SHA-256 algorithm is not available.", e);
        }
    }
}