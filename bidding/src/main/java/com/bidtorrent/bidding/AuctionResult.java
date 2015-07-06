package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;

import java.util.Collection;

public class AuctionResult {
    private IBidder winningBidder;
    private final BidResponse winningBid;
    private final float winningPrice;
    private long runnerUp;
    private Collection<BidResponse> responses;
    private long id;

    public AuctionResult(
            long id,
            BidResponse winningBid,
            float winningPrice,
            IBidder winningBidder,
            Collection<BidResponse> responses,
            long runnerUp)
    {
        this.id = id;
        this.winningBid = winningBid;
        this.winningPrice = winningPrice;
        this.winningBidder = winningBidder;
        this.responses = responses;
        this.runnerUp = runnerUp;
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

    public long getRunnerUp() {
        return runnerUp;
    }

    public long getId() {
        return id;
    }
}
