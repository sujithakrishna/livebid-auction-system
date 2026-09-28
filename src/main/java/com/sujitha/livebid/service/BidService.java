package com.sujitha.livebid.service;

import com.sujitha.livebid.dao.AuctionDAO;
import com.sujitha.livebid.dao.BidDAO;
import com.sujitha.livebid.dao.UserDAO;
import com.sujitha.livebid.exception.OptimisticLockException;
import com.sujitha.livebid.model.Auction;
import com.sujitha.livebid.model.AuctionStatus;
import com.sujitha.livebid.model.Bid;
import com.sujitha.livebid.model.Notification;
import com.sujitha.livebid.model.User;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

public class BidService {

    private final BidDAO bidDAO;
    private final AuctionDAO auctionDAO;
    private final UserDAO userDAO;
    private final NotificationService notificationService;

    public BidService() {

        this.bidDAO = new BidDAO();
        this.auctionDAO = new AuctionDAO();
        this.userDAO = new UserDAO();
        this.notificationService =
                new NotificationService();
    }

    public Bid placeBid(
            long auctionId,
            long bidderId,
            BigDecimal amount) throws SQLException {

        // ------------------------------------------------
        // Basic bid validation
        // ------------------------------------------------

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new IllegalArgumentException(
                    "Bid amount must be greater than zero."
            );
        }

        User bidder =
                userDAO.findById(bidderId);

        if (bidder == null) {

            throw new IllegalArgumentException(
                    "Bidder does not exist."
            );
        }

        /*
         * Tracks whether this bid operation encountered
         * an optimistic locking conflict.
         */
        boolean concurrencyConflictOccurred = false;

        /*
         * MySQL TIMESTAMP currently stores seconds
         * precision in the notifications table.
         *
         * Removing nanoseconds keeps the Java timestamp
         * comparison consistent with the database value.
         */
        LocalDateTime operationStartedAt =
                LocalDateTime.now()
                        .withNano(0);

        final int maxRetries = 3;

        // ------------------------------------------------
        // Optimistic locking retry loop
        // ------------------------------------------------

        for (int attempt = 1;
             attempt <= maxRetries;
             attempt++) {

            Auction auction =
                    auctionDAO.findById(auctionId);

            if (auction == null) {

                throw new IllegalArgumentException(
                        "Auction does not exist."
                );
            }

            // ------------------------------------------------
            // Auction status validation
            // ------------------------------------------------

            if (auction.getStatus()
                    != AuctionStatus.LIVE) {

                throw new IllegalStateException(
                        "Bids can only be placed on live auctions."
                );
            }

            LocalDateTime now =
                    LocalDateTime.now();

            if (now.isBefore(
                    auction.getStartTime())) {

                throw new IllegalStateException(
                        "Auction has not started yet."
                );
            }

            if (!now.isBefore(
                    auction.getEndTime())) {

                throw new IllegalStateException(
                        "Auction has already ended."
                );
            }

            // ------------------------------------------------
            // Seller cannot bid
            // ------------------------------------------------

            if (auction.getSellerId()
                    == bidderId) {

                throw new IllegalArgumentException(
                        "Seller cannot bid on their own auction."
                );
            }

            // ------------------------------------------------
            // Bid amount validation
            // ------------------------------------------------

            if (amount.compareTo(
                    auction.getCurrentHighestBid()
            ) <= 0) {

                /*
                 * If another bidder changed the auction
                 * during this bid operation, this is an
                 * actual concurrent-bid loss.
                 */
                if (concurrencyConflictOccurred) {

                    notifyConcurrentLoserIfNeeded(
                            bidderId,
                            auctionId,
                            operationStartedAt
                    );

                    throw new IllegalArgumentException(
                            "Your bid of "
                                    + amount
                                    + " was outbid. "
                                    + "Current highest bid is "
                                    + auction
                                    .getCurrentHighestBid()
                                    + ". Increase your bid "
                                    + "or leave the auction."
                    );
                }

                /*
                 * Ordinary bid that was simply too low.
                 */
                throw new IllegalArgumentException(
                        "Your bid must be greater than the "
                                + "current highest bid of "
                                + auction
                                .getCurrentHighestBid()
                );
            }

            // ------------------------------------------------
            // Capture previous highest bidder
            // ------------------------------------------------

            Long previousHighestBidderId =
                    auction.getCurrentHighestBidderId();

            BigDecimal previousHighestBid =
                    auction.getCurrentHighestBid();

            Bid bid =
                    new Bid(
                            auctionId,
                            bidderId,
                            amount
                    );

            try {

                Bid savedBid =
                        bidDAO.placeBid(
                                bid,
                                auction.getVersion()
                        );

                // ------------------------------------------------
                // Bid successfully placed
                // ------------------------------------------------

                System.out.println(
                        "Bid successfully placed."
                );

                System.out.println(
                        "Previous highest bid: "
                                + previousHighestBid
                );

                System.out.println(
                        "New highest bid: "
                                + amount
                );

                // ------------------------------------------------
                // Notify previous highest bidder
                // ------------------------------------------------

                if (previousHighestBidderId != null &&
                        previousHighestBidderId
                                != bidderId) {

                    createOutbidNotification(
                            previousHighestBidderId,
                            auctionId
                    );

                    System.out.println(
                            "OUTBID notification sent to User #"
                                    + previousHighestBidderId
                    );
                }

                return savedBid;

            } catch (OptimisticLockException e) {

                /*
                 * Another bidder successfully updated the
                 * auction before this transaction.
                 */
                concurrencyConflictOccurred = true;

                System.out.println(
                        "Optimistic lock conflict detected."
                );

                // ------------------------------------------------
                // Maximum retry attempts reached
                // ------------------------------------------------

                if (attempt == maxRetries) {

                    Auction latestAuction =
                            auctionDAO.findById(
                                    auctionId
                            );

                    if (latestAuction != null &&
                            amount.compareTo(
                                    latestAuction
                                            .getCurrentHighestBid()
                            ) <= 0) {

                        notifyConcurrentLoserIfNeeded(
                                bidderId,
                                auctionId,
                                operationStartedAt
                        );

                        throw new SQLException(
                                "Your bid was outbid after "
                                        + "concurrent bidding. "
                                        + "Current highest bid is "
                                        + latestAuction
                                        .getCurrentHighestBid()
                                        + ". Increase your bid "
                                        + "or leave the auction.",
                                e
                        );
                    }

                    throw new SQLException(
                            "Bid could not be completed after "
                                    + maxRetries
                                    + " concurrency retries.",
                            e
                    );
                }

                System.out.println(
                        "Retrying bid... Attempt "
                                + (attempt + 1)
                );
            }
        }

        throw new SQLException(
                "Bid could not be completed."
        );
    }

    // ------------------------------------------------
    // Notify concurrent losing bidder
    // ------------------------------------------------

    private void notifyConcurrentLoserIfNeeded(
            long userId,
            long auctionId,
            LocalDateTime operationStartedAt)
            throws SQLException {

        Notification latestNotification =
                notificationService
                        .getLatestNotification(
                                userId,
                                auctionId,
                                "OUTBID"
                        );

        /*
         * If an OUTBID notification was already generated
         * during this same bid operation, don't create
         * another notification.
         */
        if (latestNotification != null &&
                latestNotification.getSentAt() != null &&
                !latestNotification
                        .getSentAt()
                        .isBefore(operationStartedAt)) {

            System.out.println(
                    "OUTBID notification already exists "
                            + "for this concurrent bid."
            );

            return;
        }

        createOutbidNotification(
                userId,
                auctionId
        );

        System.out.println(
                "OUTBID notification sent to concurrent "
                        + "loser User #"
                        + userId
        );
    }

    // ------------------------------------------------
    // Create OUTBID notification
    // ------------------------------------------------

    private void createOutbidNotification(
            long userId,
            long auctionId) throws SQLException {

        notificationService.createNotification(
                userId,
                auctionId,
                "OUTBID"
        );
    }

    // ------------------------------------------------
    // Bid history
    // ------------------------------------------------

    public List<Bid> getBidHistory(
            long auctionId) throws SQLException {

        Auction auction =
                auctionDAO.findById(auctionId);

        if (auction == null) {

            throw new IllegalArgumentException(
                    "Auction does not exist."
            );
        }

        return bidDAO.findByAuctionId(
                auctionId
        );
    }
}