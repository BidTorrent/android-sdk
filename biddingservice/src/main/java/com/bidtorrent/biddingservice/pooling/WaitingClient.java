package com.bidtorrent.biddingservice.pooling;

import java.util.Date;

public class WaitingClient
{
    private final Date expirationDate;
    private int id;

    public WaitingClient(Date expirationDate, int id) {
        this.expirationDate = expirationDate;
        this.id = id;
    }

    public int getId() {
        return id;
    }

    public boolean isExpired(){
        return expirationDate.before(new Date());
    }
}