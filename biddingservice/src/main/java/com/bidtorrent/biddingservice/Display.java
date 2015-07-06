package com.bidtorrent.biddingservice;

public class Display {
    private int requesterId;
    private String fileName;
    private String notificationUrl;

    public Display(int requesterId, String fileName, String notificationUrl) {
        this.requesterId = requesterId;
        this.fileName = fileName;
        this.notificationUrl = notificationUrl;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }
}
