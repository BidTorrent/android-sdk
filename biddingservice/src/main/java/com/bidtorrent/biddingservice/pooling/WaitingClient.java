package com.bidtorrent.biddingservice.pooling;

import com.google.common.base.Function;

import java.util.Date;

public class WaitingClient extends ExpiringItem {
    private int id;
    private Function<WaitingClient, Boolean> triggerClientPassback;

    public WaitingClient(Date expirationDate, int id, Function<WaitingClient, Boolean> triggerClientPassback) {
        super(expirationDate);
        this.id = id;
        this.triggerClientPassback = triggerClientPassback;
    }

    public int getId() {
        return id;
    }

    @Override
    public void expire() {
        try {
            this.triggerClientPassback.apply(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}