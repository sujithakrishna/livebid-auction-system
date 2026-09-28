package com.sujitha.livebid;

import com.sujitha.livebid.config.DatabaseConnection;
import com.sujitha.livebid.model.Auction;
import com.sujitha.livebid.model.Notification;
import com.sujitha.livebid.service.AuctionService;
import com.sujitha.livebid.service.NotificationService;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.concurrent.CountDownLatch;

public class Main {

    public static void main(String[] args) {

        System.out.println("========================================");
        System.out.println(" LIVEBID CONCURRENT AUCTION CLOSE TEST");
        System.out.println("========================================");

        long auctionId = 3;
        long winnerId = 2;

        try {

            // -------------------------------------------------
            // STEP 1: Reset Auction #3
            // -------------------------------------------------

            String sql = """
                    UPDATE auctions
                    SET status = 'LIVE',
                        end_time = DATE_SUB(NOW(), INTERVAL 1 MINUTE)
                    WHERE id = ?
                    """;

            try (Connection connection =
                         DatabaseConnection.getConnection();
                 PreparedStatement statement =
                         connection.prepareStatement(sql)) {

                statement.setLong(1, auctionId);

                int rowsUpdated =
                        statement.executeUpdate();

                if (rowsUpdated == 0) {
                    throw new RuntimeException(
                            "Auction #3 was not found."
                    );
                }
            }

            System.out.println();
            System.out.println(
                    "Auction #" + auctionId
                            + " reset to LIVE and expired."
            );

            // -------------------------------------------------
            // STEP 2: Verify auction before test
            // -------------------------------------------------

            AuctionService verificationService =
                    new AuctionService();

            Auction beforeTest =
                    verificationService
                            .findAuctionById(auctionId);

            System.out.println();
            System.out.println(
                    "AUCTION BEFORE CONCURRENT CLOSE:"
            );

            System.out.println(
                    "Auction ID: "
                            + beforeTest.getId()
            );

            System.out.println(
                    "Status: "
                            + beforeTest.getStatus()
            );

            System.out.println(
                    "Current highest bid: ₹"
                            + beforeTest
                            .getCurrentHighestBid()
            );

            System.out.println(
                    "Current highest bidder: User #"
                            + beforeTest
                            .getCurrentHighestBidderId()
            );

            System.out.println(
                    "End time: "
                            + beforeTest.getEndTime()
            );

            // -------------------------------------------------
            // STEP 3: Create two independent services
            // -------------------------------------------------

            AuctionService service1 =
                    new AuctionService();

            AuctionService service2 =
                    new AuctionService();

            CountDownLatch readyLatch =
                    new CountDownLatch(2);

            CountDownLatch startLatch =
                    new CountDownLatch(1);

            // -------------------------------------------------
            // THREAD 1
            // -------------------------------------------------

            Thread thread1 = new Thread(() -> {

                try {

                    readyLatch.countDown();

                    startLatch.await();

                    System.out.println(
                            "[Thread 1] Attempting to close..."
                    );

                    Auction result =
                            service1
                                    .closeAuctionIfEnded(
                                            auctionId
                                    );

                    System.out.println(
                            "[Thread 1] Result: "
                                    + result.getStatus()
                    );

                } catch (Exception e) {

                    System.out.println(
                            "[Thread 1] ERROR"
                    );

                    e.printStackTrace();
                }
            });

            // -------------------------------------------------
            // THREAD 2
            // -------------------------------------------------

            Thread thread2 = new Thread(() -> {

                try {

                    readyLatch.countDown();

                    startLatch.await();

                    System.out.println(
                            "[Thread 2] Attempting to close..."
                    );

                    Auction result =
                            service2
                                    .closeAuctionIfEnded(
                                            auctionId
                                    );

                    System.out.println(
                            "[Thread 2] Result: "
                                    + result.getStatus()
                    );

                } catch (Exception e) {

                    System.out.println(
                            "[Thread 2] ERROR"
                    );

                    e.printStackTrace();
                }
            });

            // -------------------------------------------------
            // STEP 4: Start both threads
            // -------------------------------------------------

            thread1.start();
            thread2.start();

            readyLatch.await();

            System.out.println();
            System.out.println(
                    "Both threads ready..."
            );

            System.out.println(
                    "Starting concurrent close..."
            );

            startLatch.countDown();

            // -------------------------------------------------
            // STEP 5: Wait for both threads
            // -------------------------------------------------

            thread1.join();
            thread2.join();

            // -------------------------------------------------
            // STEP 6: Check final auction state
            // -------------------------------------------------

            AuctionService finalService =
                    new AuctionService();

            Auction finalAuction =
                    finalService
                            .findAuctionById(auctionId);

            System.out.println();
            System.out.println(
                    "FINAL AUCTION STATE:"
            );

            System.out.println(
                    "Auction ID: "
                            + finalAuction.getId()
            );

            System.out.println(
                    "Status: "
                            + finalAuction.getStatus()
            );

            System.out.println(
                    "Current highest bid: ₹"
                            + finalAuction
                            .getCurrentHighestBid()
            );

            System.out.println(
                    "Current highest bidder: User #"
                            + finalAuction
                            .getCurrentHighestBidderId()
            );

            // -------------------------------------------------
            // STEP 7: Check WON notification
            // -------------------------------------------------

            NotificationService notificationService =
                    new NotificationService();

            Notification winnerNotification =
                    notificationService
                            .getLatestNotification(
                                    winnerId,
                                    auctionId,
                                    "WON"
                            );

            System.out.println();
            System.out.println(
                    "WINNER NOTIFICATION:"
            );

            if (winnerNotification != null) {

                System.out.println(
                        winnerNotification
                );

            } else {

                System.out.println(
                        "ERROR: WON notification not found."
                );
            }

            // -------------------------------------------------
            // STEP 8: Count WON notifications
            // -------------------------------------------------

            int wonNotificationCount = 0;

            for (Notification notification :
                    notificationService
                            .getUserNotifications(
                                    winnerId
                            )) {

                if (notification.getAuctionId()
                        == auctionId
                        && "WON".equals(
                        notification.getType())) {

                    wonNotificationCount++;
                }
            }

            System.out.println();
            System.out.println(
                    "WON NOTIFICATION COUNT FOR "
                            + "AUCTION #"
                            + auctionId
                            + ": "
                            + wonNotificationCount
            );

            // -------------------------------------------------
            // FINAL RESULT
            // -------------------------------------------------

            System.out.println();

            if (finalAuction.getStatus()
                    .name()
                    .equals("CLOSED")
                    && finalAuction
                    .getCurrentHighestBidderId()
                    != null
                    && wonNotificationCount == 1) {

                System.out.println(
                        "CONCURRENCY TEST PASSED."
                );

                System.out.println(
                        "Only one WON notification exists."
                );

            } else {

                System.out.println(
                        "CONCURRENCY TEST FAILED."
                );
            }

            System.out.println();
            System.out.println(
                    "CONCURRENT AUCTION CLOSE TEST COMPLETED"
            );

        } catch (Exception e) {

            System.out.println();
            System.out.println(
                    "TEST FAILED"
            );

            e.printStackTrace();
        }
    }
}