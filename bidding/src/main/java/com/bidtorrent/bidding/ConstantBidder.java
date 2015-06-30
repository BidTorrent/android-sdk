package com.bidtorrent.bidding;

import java.util.concurrent.Callable;

public class ConstantBidder implements IBidder {
    private long id;
    private final BidResponse response;

    public ConstantBidder(long id, BidResponse response) {
        this.id = id;
        this.response = response;
    }

    @Override
    public Callable<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback) {
        return new Callable<BidResponse>() {
            @Override
            public BidResponse call() throws Exception {
                return response;
            }
        };
    }

    @Override
    public long getId() {
        return this.id;
    }
}
