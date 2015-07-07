package com.bidtorrent.biddingservice.pooling;

import com.bidtorrent.bidding.AuctionResult;

import java.util.Date;

public class ReadyAd extends ExpiringItem {
    private final AuctionResult result;
    private final String cacheFileName;

    public ReadyAd(AuctionResult result, String cacheFileName, Date expirationDate) {
        super(expirationDate);
        this.result = result;
        this.cacheFileName = cacheFileName;
    }

    public AuctionResult getResult() {
        return result;
    }

    public String getCacheFileName() {
        return cacheFileName;
    }
}