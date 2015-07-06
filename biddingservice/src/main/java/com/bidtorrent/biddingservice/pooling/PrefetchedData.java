package com.bidtorrent.biddingservice.pooling;

import java.util.Date;

public class PrefetchedData {

    private String fileName;
    private String notificationUrl;
    private Date expirationDate;

    public PrefetchedData(String fileName, String notificationUrl, Date expirationDate) {
        this.fileName = fileName;
        this.notificationUrl = notificationUrl;
        this.expirationDate = expirationDate;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isExpired(){
        return expirationDate.before(new Date());
    }

    public String getNotificationUrl() {
        return notificationUrl;
    }
}
