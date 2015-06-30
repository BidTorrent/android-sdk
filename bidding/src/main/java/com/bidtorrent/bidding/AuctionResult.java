package com.bidtorrent.bidding;

import java.util.Collection;
import java.util.List;

public class AuctionResult {
    private IBidder winningBidder;
    private final BidResponse winningBid;
    private final float winningPrice;
    private Collection<BidResponse> responses;

    public AuctionResult(BidResponse winningBid, float winningPrice, IBidder winningBidder, Collection<BidResponse> responses)
    {
        this.winningBid = winningBid;
        this.winningPrice = winningPrice;
        this.winningBidder = winningBidder;
        this.responses = responses;
    }

    public BidResponse getWinningBid() {
        return winningBid;
    }

    public float getWinningPrice() {
        return winningPrice;
    }

    public Collection<BidResponse> getResponses() {
        return responses;
    }

    public IBidder getWinningBidder() {
        return winningBidder;
    }
}
