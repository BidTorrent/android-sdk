package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.ContextualizedBidResponse;

import java.io.Serializable;
import java.util.Collection;

public class AuctionResult implements Serializable {
    private final ContextualizedBidResponse winningBid;
    private final ContextualizedBidResponse runnerUp;
    private Collection<ContextualizedBidResponse> responses;
    private float winningPrice;

    public AuctionResult(
            ContextualizedBidResponse winningBid,
            Collection<ContextualizedBidResponse> responses,
            ContextualizedBidResponse runnerUp,
            float winningPrice){
        this.winningBid = winningBid;
        this.responses = responses;
        this.runnerUp = runnerUp;
        this.winningPrice = winningPrice;
    }

    public AuctionResult() {
        this(null,null,null, 0f);
    }

    public ContextualizedBidResponse getWinningBid() {
        return winningBid;
    }

    public Collection<ContextualizedBidResponse> getResponses() {
        return responses;
    }

    public ContextualizedBidResponse getRunnerUp() {
        return runnerUp;
    }
}
