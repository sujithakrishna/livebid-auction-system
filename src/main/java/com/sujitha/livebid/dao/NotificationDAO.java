package com.sujitha.livebid.dao;

import com.sujitha.livebid.config.DatabaseConnection;
import com.sujitha.livebid.model.Notification;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {

    // ------------------------------------------------
    // Save notification
    // ------------------------------------------------

    public Notification save(
            Notification notification) throws SQLException {

        String sql = """
                INSERT INTO notifications (
                    user_id,
                    auction_id,
                    type
                )
                VALUES (?, ?, ?)
                """;

        long generatedId;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(
                             sql,
                             Statement.RETURN_GENERATED_KEYS)) {

            statement.setLong(
                    1,
                    notification.getUserId()
            );

            statement.setLong(
                    2,
                    notification.getAuctionId()
            );

            statement.setString(
                    3,
                    notification.getType()
            );

            int rowsInserted =
                    statement.executeUpdate();

            if (rowsInserted == 0) {

                throw new SQLException(
                        "Notification could not be created."
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {

                    generatedId =
                            generatedKeys.getLong(1);

                } else {

                    throw new SQLException(
                            "Notification was inserted "
                                    + "but no ID was generated."
                    );
                }
            }
        }

        Notification savedNotification =
                findById(generatedId);

        if (savedNotification == null) {

            throw new SQLException(
                    "Notification was inserted "
                            + "but could not be retrieved."
            );
        }

        return savedNotification;
    }

    // ------------------------------------------------
    // Find notification by ID
    // ------------------------------------------------

    public Notification findById(
            long id) throws SQLException {

        String sql = """
                SELECT
                    id,
                    user_id,
                    auction_id,
                    type,
                    sent_at
                FROM notifications
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

                    return mapNotification(
                            resultSet
                    );
                }
            }
        }

        return null;
    }

    // ------------------------------------------------
    // Find all notifications for a user
    // ------------------------------------------------

    public List<Notification> findByUserId(
            long userId) throws SQLException {

        String sql = """
                SELECT
                    id,
                    user_id,
                    auction_id,
                    type,
                    sent_at
                FROM notifications
                WHERE user_id = ?
                ORDER BY sent_at DESC, id DESC
                """;

        List<Notification> notifications =
                new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    notifications.add(
                            mapNotification(
                                    resultSet
                            )
                    );
                }
            }
        }

        return notifications;
    }

    // ------------------------------------------------
    // Find latest notification for a user,
    // auction and notification type
    // ------------------------------------------------

    public Notification findLatestByUserAndAuctionAndType(
            long userId,
            long auctionId,
            String type) throws SQLException {

        String sql = """
                SELECT
                    id,
                    user_id,
                    auction_id,
                    type,
                    sent_at
                FROM notifications
                WHERE user_id = ?
                  AND auction_id = ?
                  AND type = ?
                ORDER BY sent_at DESC, id DESC
                LIMIT 1
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(1, userId);
            statement.setLong(2, auctionId);
            statement.setString(3, type);

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                if (resultSet.next()) {

                    return mapNotification(
                            resultSet
                    );
                }
            }
        }

        return null;
    }

    // ------------------------------------------------
    // Map ResultSet → Notification
    // ------------------------------------------------

    private Notification mapNotification(
            ResultSet resultSet)
            throws SQLException {

        Timestamp timestamp =
                resultSet.getTimestamp("sent_at");

        LocalDateTime sentAt = null;

        if (timestamp != null) {

            sentAt =
                    timestamp.toLocalDateTime();
        }

        return new Notification(
                resultSet.getLong("id"),
                resultSet.getLong("user_id"),
                resultSet.getLong("auction_id"),
                resultSet.getString("type"),
                sentAt
        );
    }
}