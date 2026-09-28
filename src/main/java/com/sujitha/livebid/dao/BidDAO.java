package com.sujitha.livebid.dao;

import com.sujitha.livebid.config.DatabaseConnection;
import com.sujitha.livebid.exception.OptimisticLockException;
import com.sujitha.livebid.model.AuctionStatus;
import com.sujitha.livebid.model.Bid;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    public Bid placeBid(
            Bid bid,
            long expectedVersion) throws SQLException {

        String auctionSql = """
                SELECT
                    seller_id,
                    current_highest_bid,
                    start_time,
                    end_time,
                    status,
                    version
                FROM auctions
                WHERE id = ?
                """;

        String updateAuctionSql = """
                UPDATE auctions
                SET
                    current_highest_bid = ?,
                    current_highest_bidder_id = ?,
                    version = version + 1
                WHERE id = ?
                  AND version = ?
                  AND current_highest_bid < ?
                """;

        String insertBidSql = """
                INSERT INTO bids (
                    auction_id,
                    bidder_id,
                    amount,
                    is_auto_bid
                )
                VALUES (?, ?, ?, ?)
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection()) {

            connection.setAutoCommit(false);

            try {

                long sellerId;
                BigDecimal currentHighestBid;
                AuctionStatus status;
                LocalDateTime startTime;
                LocalDateTime endTime;

                /*
                 * Read the latest auction state inside
                 * the same transaction.
                 */
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     auctionSql)) {

                    statement.setLong(
                            1,
                            bid.getAuctionId()
                    );

                    try (ResultSet resultSet =
                                 statement.executeQuery()) {

                        if (!resultSet.next()) {

                            throw new SQLException(
                                    "Auction does not exist."
                            );
                        }

                        sellerId =
                                resultSet.getLong(
                                        "seller_id"
                                );

                        currentHighestBid =
                                resultSet.getBigDecimal(
                                        "current_highest_bid"
                                );

                        status =
                                AuctionStatus.valueOf(
                                        resultSet.getString(
                                                "status"
                                        )
                                );

                        startTime =
                                resultSet.getTimestamp(
                                        "start_time"
                                ).toLocalDateTime();

                        endTime =
                                resultSet.getTimestamp(
                                        "end_time"
                                ).toLocalDateTime();
                    }
                }

                /*
                 * Validate seller.
                 */
                if (sellerId == bid.getBidderId()) {

                    throw new IllegalArgumentException(
                            "Seller cannot bid on their own auction."
                    );
                }

                /*
                 * Auction must be LIVE.
                 */
                if (status != AuctionStatus.LIVE) {

                    throw new IllegalStateException(
                            "Bids can only be placed on live auctions."
                    );
                }

                LocalDateTime now =
                        LocalDateTime.now();

                /*
                 * Auction must have started.
                 */
                if (now.isBefore(startTime)) {

                    throw new IllegalStateException(
                            "Auction has not started yet."
                    );
                }

                /*
                 * Auction must not have ended.
                 */
                if (!now.isBefore(endTime)) {

                    throw new IllegalStateException(
                            "Auction has already ended."
                    );
                }

                /*
                 * Bid must be greater than the latest
                 * highest bid read from the database.
                 */
                if (bid.getAmount() == null ||
                        bid.getAmount().compareTo(
                                currentHighestBid
                        ) <= 0) {

                    throw new IllegalArgumentException(
                            "Bid must be greater than the current highest bid of "
                                    + currentHighestBid
                    );
                }

                /*
                 * Optimistic locking.
                 *
                 * The UPDATE succeeds only if:
                 *
                 * 1. The auction ID matches.
                 * 2. The version is still the version
                 *    we expected.
                 * 3. The new bid is still higher than
                 *    the current highest bid.
                 */
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     updateAuctionSql)) {

                    statement.setBigDecimal(
                            1,
                            bid.getAmount()
                    );

                    statement.setLong(
                            2,
                            bid.getBidderId()
                    );

                    statement.setLong(
                            3,
                            bid.getAuctionId()
                    );

                    statement.setLong(
                            4,
                            expectedVersion
                    );

                    statement.setBigDecimal(
                            5,
                            bid.getAmount()
                    );

                    int rowsUpdated =
                            statement.executeUpdate();

                    /*
                     * Zero rows means another bidder changed
                     * the auction first.
                     */
                    if (rowsUpdated == 0) {

                        connection.rollback();

                        throw new OptimisticLockException(
                                "Auction was updated by another bidder."
                        );
                    }
                }

                /*
                 * Only record the bid after the auction update
                 * succeeds.
                 */
                try (PreparedStatement statement =
                             connection.prepareStatement(
                                     insertBidSql,
                                     Statement.RETURN_GENERATED_KEYS)) {

                    statement.setLong(
                            1,
                            bid.getAuctionId()
                    );

                    statement.setLong(
                            2,
                            bid.getBidderId()
                    );

                    statement.setBigDecimal(
                            3,
                            bid.getAmount()
                    );

                    statement.setBoolean(
                            4,
                            bid.isAutoBid()
                    );

                    int rowsInserted =
                            statement.executeUpdate();

                    if (rowsInserted == 0) {

                        connection.rollback();

                        throw new SQLException(
                                "Bid could not be recorded."
                        );
                    }

                    try (ResultSet generatedKeys =
                                 statement.getGeneratedKeys()) {

                        if (generatedKeys.next()) {

                            bid.setId(
                                    generatedKeys.getLong(1)
                            );

                        } else {

                            connection.rollback();

                            throw new SQLException(
                                    "Bid was inserted but no ID was generated."
                            );
                        }
                    }
                }

                /*
                 * Both operations succeeded:
                 *
                 * UPDATE auctions
                 * INSERT bids
                 *
                 * Commit them together.
                 */
                connection.commit();

                bid.setPlacedAt(
                        LocalDateTime.now()
                );

                return bid;

            } catch (Exception e) {

                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    e.addSuppressed(rollbackException);
                }

                throw e;
            }

        }
    }


    public List<Bid> findByAuctionId(
            long auctionId) throws SQLException {

        String sql = """
                SELECT
                    id,
                    auction_id,
                    bidder_id,
                    amount,
                    placed_at,
                    is_auto_bid
                FROM bids
                WHERE auction_id = ?
                ORDER BY placed_at DESC, id DESC
                """;

        List<Bid> bids = new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    auctionId
            );

            try (ResultSet resultSet =
                         statement.executeQuery()) {

                while (resultSet.next()) {

                    bids.add(
                            mapBid(resultSet)
                    );
                }
            }
        }

        return bids;
    }


    private Bid mapBid(
            ResultSet resultSet) throws SQLException {

        Timestamp placedTimestamp =
                resultSet.getTimestamp("placed_at");

        LocalDateTime placedAt = null;

        if (placedTimestamp != null) {
            placedAt =
                    placedTimestamp.toLocalDateTime();
        }

        return new Bid(
                resultSet.getLong("id"),
                resultSet.getLong("auction_id"),
                resultSet.getLong("bidder_id"),
                resultSet.getBigDecimal("amount"),
                placedAt,
                resultSet.getBoolean("is_auto_bid")
        );
    }
}