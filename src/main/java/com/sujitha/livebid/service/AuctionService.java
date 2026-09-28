package com.sujitha.livebid.service;

import com.sujitha.livebid.dao.AuctionDAO;
import com.sujitha.livebid.model.Auction;
import com.sujitha.livebid.model.AuctionStatus;
import com.sujitha.livebid.model.Notification;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class AuctionService {

    private final AuctionDAO auctionDAO;
    private final NotificationService notificationService;

    public AuctionService() {
        this.auctionDAO =
                new AuctionDAO();

        this.notificationService =
                new NotificationService();
    }

    // ---------------------------------------------------------
    // CREATE AUCTION
    // ---------------------------------------------------------

    public Auction createAuction(
            Auction auction) throws SQLException {

        if (auction == null) {
            throw new IllegalArgumentException(
                    "Auction cannot be null."
            );
        }

        if (auction.getSellerId() <= 0) {
            throw new IllegalArgumentException(
                    "Invalid seller ID."
            );
        }

        if (auction.getTitle() == null ||
                auction.getTitle().isBlank()) {

            throw new IllegalArgumentException(
                    "Auction title cannot be empty."
            );
        }

        if (auction.getStartingPrice() == null ||
                auction.getStartingPrice()
                        .compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Starting price must be greater than zero."
            );
        }

        if (auction.getStartTime() == null ||
                auction.getEndTime() == null) {

            throw new IllegalArgumentException(
                    "Start time and end time are required."
            );
        }

        if (!auction.getEndTime()
                .isAfter(auction.getStartTime())) {

            throw new IllegalArgumentException(
                    "End time must be after start time."
            );
        }

        return auctionDAO.save(auction);
    }

    // ---------------------------------------------------------
    // FIND AUCTION
    // ---------------------------------------------------------

    public Auction findAuctionById(
            long auctionId) throws SQLException {

        if (auctionId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid auction ID."
            );
        }

        return auctionDAO.findById(auctionId);
    }

    // ---------------------------------------------------------
    // FIND ALL AUCTIONS
    // ---------------------------------------------------------

    public List<Auction> findAllAuctions()
            throws SQLException {

        return auctionDAO.findAll();
    }

    // ---------------------------------------------------------
    // UPDATE AUCTION STATUS
    // ---------------------------------------------------------

    public Auction updateAuctionStatus(
            long auctionId,
            AuctionStatus status)
            throws SQLException {

        if (auctionId <= 0) {
            throw new IllegalArgumentException(
                    "Invalid auction ID."
            );
        }

        if (status == null) {
            throw new IllegalArgumentException(
                    "Auction status cannot be null."
            );
        }

        auctionDAO.updateStatus(
                auctionId,
                status
        );

        return auctionDAO.findById(auctionId);
    }

    // ---------------------------------------------------------
    // START AUCTION
    // ---------------------------------------------------------

    public Auction startAuction(
            long auctionId) throws SQLException {

        Auction auction =
                findAuctionById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException(
                    "Auction not found."
            );
        }

        if (auction.getStatus()
                == AuctionStatus.CLOSED) {

            throw new IllegalStateException(
                    "Closed auction cannot be started."
            );
        }

        auctionDAO.updateStatus(
                auctionId,
                AuctionStatus.LIVE
        );

        return auctionDAO.findById(auctionId);
    }

    // ---------------------------------------------------------
    // CLOSE AUCTION IF END TIME HAS PASSED
    // ---------------------------------------------------------

    public Auction closeAuctionIfEnded(
            long auctionId) throws SQLException {

        Auction auction =
                auctionDAO.findById(auctionId);

        if (auction == null) {
            throw new IllegalArgumentException(
                    "Auction not found."
            );
        }

        // Already closed.
        if (auction.getStatus()
                == AuctionStatus.CLOSED) {

            return auction;
        }

        // Auction has not ended yet.
        if (auction.getEndTime() == null ||
                LocalDateTime.now()
                        .isBefore(auction.getEndTime())) {

            return auction;
        }

        /*
         * IMPORTANT:
         *
         * This is now an atomic LIVE -> CLOSED operation.
         *
         * Only one concurrent caller can receive true.
         */
        boolean closedSuccessfully =
                auctionDAO.closeIfLive(auctionId);

        /*
         * Another thread already closed the auction.
         *
         * Therefore this thread must NOT create
         * another winner notification.
         */
        if (!closedSuccessfully) {

            return auctionDAO.findById(
                    auctionId
            );
        }

        /*
         * This thread successfully changed:
         *
         * LIVE -> CLOSED
         *
         * Therefore it is the only thread responsible
         * for processing the winner notification.
         */

        Auction closedAuction =
                auctionDAO.findById(auctionId);

        if (closedAuction == null) {
            throw new SQLException(
                    "Auction was closed but could "
                            + "not be retrieved."
            );
        }

        Long winnerId =
                closedAuction
                        .getCurrentHighestBidderId();

        // -----------------------------------------------------
        // NO WINNER
        // -----------------------------------------------------

        if (winnerId == null) {

            System.out.println(
                    "Auction closed without a winner."
            );

            return closedAuction;
        }

        // -----------------------------------------------------
        // WINNER FOUND
        // -----------------------------------------------------

        Notification winnerNotification =
                notificationService
                        .createNotificationIfNotExists(
                                winnerId,
                                auctionId,
                                "WON"
                        );

        System.out.println(
                "Winner notification processed "
                        + "for User #"
                        + winnerId
        );

        System.out.println(
                "WON notification: "
                        + winnerNotification
        );

        return closedAuction;
    }

    // ---------------------------------------------------------
    // CLOSE ALL EXPIRED AUCTIONS
    // ---------------------------------------------------------

    public void closeExpiredAuctions()
            throws SQLException {

        List<Auction> auctions =
                auctionDAO.findAll();

        LocalDateTime now =
                LocalDateTime.now();

        for (Auction auction : auctions) {

            if (auction.getStatus()
                    != AuctionStatus.LIVE) {

                continue;
            }

            if (auction.getEndTime() == null) {
                continue;
            }

            if (now.isBefore(
                    auction.getEndTime())) {

                continue;
            }

            closeAuctionIfEnded(
                    auction.getId()
            );
        }
    }
}