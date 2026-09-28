package com.sujitha.livebid.model;

import java.time.LocalDateTime;

public class Notification {

    private long id;
    private long userId;
    private long auctionId;
    private String type;
    private LocalDateTime sentAt;

    public Notification() {
    }

    public Notification(
            long userId,
            long auctionId,
            String type) {

        this.userId = userId;
        this.auctionId = auctionId;
        this.type = type;
    }

    public Notification(
            long id,
            long userId,
            long auctionId,
            String type,
            LocalDateTime sentAt) {

        this.id = id;
        this.userId = userId;
        this.auctionId = auctionId;
        this.type = type;
        this.sentAt = sentAt;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public long getUserId() {
        return userId;
    }

    public void setUserId(long userId) {
        this.userId = userId;
    }

    public long getAuctionId() {
        return auctionId;
    }

    public void setAuctionId(long auctionId) {
        this.auctionId = auctionId;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public LocalDateTime getSentAt() {
        return sentAt;
    }

    public void setSentAt(LocalDateTime sentAt) {
        this.sentAt = sentAt;
    }

    @Override
    public String toString() {
        return "Notification{" +
                "id=" + id +
                ", userId=" + userId +
                ", auctionId=" + auctionId +
                ", type='" + type + '\'' +
                ", sentAt=" + sentAt +
                '}';
    }
}