package com.sujitha.livebid.dao;

import com.sujitha.livebid.config.DatabaseConnection;
import com.sujitha.livebid.model.Auction;
import com.sujitha.livebid.model.AuctionStatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AuctionDAO {

    // ---------------------------------------------------------
    // SAVE AUCTION
    // ---------------------------------------------------------

    public Auction save(Auction auction) throws SQLException {

        String sql = """
                INSERT INTO auctions (
                    seller_id,
                    title,
                    description,
                    image_url,
                    starting_price,
                    current_highest_bid,
                    current_highest_bidder_id,
                    start_time,
                    end_time,
                    status,
                    version
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
                    auction.getSellerId()
            );

            statement.setString(
                    2,
                    auction.getTitle()
            );

            statement.setString(
                    3,
                    auction.getDescription()
            );

            statement.setString(
                    4,
                    auction.getImageUrl()
            );

            statement.setBigDecimal(
                    5,
                    auction.getStartingPrice()
            );

            statement.setBigDecimal(
                    6,
                    auction.getCurrentHighestBid()
            );

            if (auction.getCurrentHighestBidderId()
                    == null) {

                statement.setNull(
                        7,
                        java.sql.Types.BIGINT
                );

            } else {

                statement.setLong(
                        7,
                        auction.getCurrentHighestBidderId()
                );
            }

            statement.setTimestamp(
                    8,
                    Timestamp.valueOf(
                            auction.getStartTime()
                    )
            );

            statement.setTimestamp(
                    9,
                    Timestamp.valueOf(
                            auction.getEndTime()
                    )
            );

            statement.setString(
                    10,
                    auction.getStatus().name()
            );

            statement.setLong(
                    11,
                    auction.getVersion()
            );

            int rowsInserted =
                    statement.executeUpdate();

            if (rowsInserted == 0) {
                throw new SQLException(
                        "Auction could not be created."
                );
            }

            try (ResultSet generatedKeys =
                         statement.getGeneratedKeys()) {

                if (generatedKeys.next()) {

                    generatedId =
                            generatedKeys.getLong(1);

                } else {

                    throw new SQLException(
                            "Auction was inserted "
                                    + "but no ID was generated."
                    );
                }
            }
        }

        Auction savedAuction =
                findById(generatedId);

        if (savedAuction == null) {
            throw new SQLException(
                    "Auction was inserted "
                            + "but could not be retrieved."
            );
        }

        return savedAuction;
    }

    // ---------------------------------------------------------
    // FIND AUCTION BY ID
    // ---------------------------------------------------------

    public Auction findById(long id)
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    seller_id,
                    title,
                    description,
                    image_url,
                    starting_price,
                    current_highest_bid,
                    current_highest_bidder_id,
                    start_time,
                    end_time,
                    status,
                    version
                FROM auctions
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
                    return mapAuction(resultSet);
                }
            }
        }

        return null;
    }

    // ---------------------------------------------------------
    // FIND ALL AUCTIONS
    // ---------------------------------------------------------

    public List<Auction> findAll()
            throws SQLException {

        String sql = """
                SELECT
                    id,
                    seller_id,
                    title,
                    description,
                    image_url,
                    starting_price,
                    current_highest_bid,
                    current_highest_bidder_id,
                    start_time,
                    end_time,
                    status,
                    version
                FROM auctions
                ORDER BY id
                """;

        List<Auction> auctions =
                new ArrayList<>();

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql);
             ResultSet resultSet =
                     statement.executeQuery()) {

            while (resultSet.next()) {

                auctions.add(
                        mapAuction(resultSet)
                );
            }
        }

        return auctions;
    }

    // ---------------------------------------------------------
    // UPDATE AUCTION STATUS
    // ---------------------------------------------------------

    public void updateStatus(
            long auctionId,
            AuctionStatus status)
            throws SQLException {

        String sql = """
                UPDATE auctions
                SET status = ?
                WHERE id = ?
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setString(
                    1,
                    status.name()
            );

            statement.setLong(
                    2,
                    auctionId
            );

            int rowsUpdated =
                    statement.executeUpdate();

            if (rowsUpdated == 0) {
                throw new SQLException(
                        "Auction status could not be updated."
                );
            }
        }
    }

    // ---------------------------------------------------------
    // ATOMIC LIVE -> CLOSED
    // ---------------------------------------------------------

    public boolean closeIfLive(
            long auctionId)
            throws SQLException {

        String sql = """
                UPDATE auctions
                SET status = 'CLOSED'
                WHERE id = ?
                  AND status = 'LIVE'
                """;

        try (Connection connection =
                     DatabaseConnection.getConnection();
             PreparedStatement statement =
                     connection.prepareStatement(sql)) {

            statement.setLong(
                    1,
                    auctionId
            );

            int rowsUpdated =
                    statement.executeUpdate();

            /*
             * Exactly one row updated:
             *
             * LIVE -> CLOSED
             *
             * This thread successfully closed
             * the auction.
             */
            if (rowsUpdated == 1) {
                return true;
            }

            /*
             * Zero rows updated means either:
             *
             * 1. Auction does not exist, or
             * 2. Auction is already CLOSED, or
             * 3. Auction is not LIVE.
             *
             * In all these cases, this thread did
             * not perform the LIVE -> CLOSED transition.
             */
            return false;
        }
    }

    // ---------------------------------------------------------
    // MAP RESULT SET TO AUCTION
    // ---------------------------------------------------------

    private Auction mapAuction(
            ResultSet resultSet)
            throws SQLException {

        Timestamp startTimestamp =
                resultSet.getTimestamp("start_time");

        Timestamp endTimestamp =
                resultSet.getTimestamp("end_time");

        LocalDateTime startTime =
                startTimestamp != null
                        ? startTimestamp.toLocalDateTime()
                        : null;

        LocalDateTime endTime =
                endTimestamp != null
                        ? endTimestamp.toLocalDateTime()
                        : null;

        long currentHighestBidderId =
                resultSet.getLong(
                        "current_highest_bidder_id"
                );

        Long bidderId = null;

        if (!resultSet.wasNull()) {
            bidderId =
                    currentHighestBidderId;
        }

        return new Auction(
                resultSet.getLong("id"),
                resultSet.getLong("seller_id"),
                resultSet.getString("title"),
                resultSet.getString("description"),
                resultSet.getString("image_url"),
                resultSet.getBigDecimal(
                        "starting_price"
                ),
                resultSet.getBigDecimal(
                        "current_highest_bid"
                ),
                bidderId,
                startTime,
                endTime,
                AuctionStatus.valueOf(
                        resultSet.getString("status")
                ),
                resultSet.getLong("version")
        );
    }
}