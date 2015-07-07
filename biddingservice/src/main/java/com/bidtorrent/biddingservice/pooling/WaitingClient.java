package com.bidtorrent.biddingservice.pooling;

import java.util.Date;

public class WaitingClient extends ExpiringItem {
    private int id;

    public WaitingClient(Date expirationDate, int id) {
        super(expirationDate);
        this.id = id;
    }

    public int getId() {
        return id;
    }
}