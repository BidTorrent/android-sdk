package com.bidtorrent.bidding.bidders;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.IErrorCallback;
import com.bidtorrent.bidding.messages.BidResponse;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.MoreExecutors;

import java.util.concurrent.Callable;

public class ConstantBidder implements IBidder {
    private long id;
    private final BidResponse response;

    public ConstantBidder(long id, BidResponse response) {
        this.id = id;
        this.response = response;
    }

    @Override
    public ListenableFuture<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback) {
        return Futures.immediateFuture(response);
    }

    @Override
    public long getId() {
        return this.id;
    }
}
