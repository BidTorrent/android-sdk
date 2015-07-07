package com.bidtorrent.biddingservice.pooling;

import com.bidtorrent.bidding.AuctionResult;

import java.util.Date;

public class PendingPrefetchItem extends ExpiringItem {
    private final AuctionResult result;

    public PendingPrefetchItem(AuctionResult result, Date expirationDate) {
        super(expirationDate);
        this.result = result;
    }

    public AuctionResult getResult() {
        return result;
    }
}