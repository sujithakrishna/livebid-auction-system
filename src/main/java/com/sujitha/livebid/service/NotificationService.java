package com.sujitha.livebid.service;

import com.sujitha.livebid.dao.NotificationDAO;
import com.sujitha.livebid.model.Notification;

import java.sql.SQLException;
import java.util.List;

public class NotificationService {

    private final NotificationDAO notificationDAO;

    public NotificationService() {
        this.notificationDAO =
                new NotificationDAO();
    }

    // ---------------------------------------------------------
    // CREATE NOTIFICATION
    // ---------------------------------------------------------

    public Notification createNotification(
            long userId,
            long auctionId,
            String type) throws SQLException {

        validateUserId(userId);
        validateAuctionId(auctionId);
        validateType(type);

        return notificationDAO.save(
                new Notification(
                        userId,
                        auctionId,
                        type
                )
        );
    }

    // ---------------------------------------------------------
    // GET ALL NOTIFICATIONS FOR A USER
    // ---------------------------------------------------------

    public List<Notification> getUserNotifications(
            long userId) throws SQLException {

        validateUserId(userId);

        return notificationDAO.findByUserId(
                userId
        );
    }

    // ---------------------------------------------------------
    // GET LATEST NOTIFICATION
    // ---------------------------------------------------------

    public Notification getLatestNotification(
            long userId,
            long auctionId,
            String type) throws SQLException {

        validateUserId(userId);
        validateAuctionId(auctionId);
        validateType(type);

        return notificationDAO
                .findLatestByUserAndAuctionAndType(
                        userId,
                        auctionId,
                        type
                );
    }

    // ---------------------------------------------------------
    // CHECK WHETHER NOTIFICATION ALREADY EXISTS
    // ---------------------------------------------------------

    public boolean notificationExists(
            long userId,
            long auctionId,
            String type) throws SQLException {

        return getLatestNotification(
                userId,
                auctionId,
                type
        ) != null;
    }

    // ---------------------------------------------------------
    // CREATE NOTIFICATION ONLY IF IT DOES NOT EXIST
    // ---------------------------------------------------------

    public Notification createNotificationIfNotExists(
            long userId,
            long auctionId,
            String type) throws SQLException {

        validateUserId(userId);
        validateAuctionId(auctionId);
        validateType(type);

        Notification existingNotification =
                notificationDAO
                        .findLatestByUserAndAuctionAndType(
                                userId,
                                auctionId,
                                type
                        );

        if (existingNotification != null) {
            return existingNotification;
        }

        return notificationDAO.save(
                new Notification(
                        userId,
                        auctionId,
                        type
                )
        );
    }

    // ---------------------------------------------------------
    // VALIDATION METHODS
    // ---------------------------------------------------------

    private void validateUserId(long userId) {

        if (userId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid user ID."
            );
        }
    }

    private void validateAuctionId(long auctionId) {

        if (auctionId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid auction ID."
            );
        }
    }

    private void validateType(String type) {

        if (type == null ||
                type.isBlank()) {

            throw new IllegalArgumentException(
                    "Notification type cannot be empty."
            );
        }
    }
}