package com.bidtorrent.biddingservice;

import java.util.Date;

public class PrefetchedData {

    private String fileName;
    private Date expirationDate;

    public PrefetchedData(String fileName, Date expirationDate) {
        this.fileName = fileName;
        this.expirationDate = expirationDate;
    }

    public String getFileName() {
        return fileName;
    }

    public boolean isExpired(){
        return expirationDate.before(new Date());
    }
}
