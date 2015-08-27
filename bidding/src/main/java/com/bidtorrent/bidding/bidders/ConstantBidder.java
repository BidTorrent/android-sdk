package com.bidtorrent.bidding.bidders;

import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.IErrorCallback;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.ContextualizedBidResponse;
import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

public class ConstantBidder implements IBidder {

    private long id;
    private final BidResponse response;

    public ConstantBidder(long id, BidResponse response) {
        this.id = id;
        this.response = response;
    }

    @Override
    public ListenableFuture<ContextualizedBidResponse> bid(Imp impression, IErrorCallback errorCallback) {
        BidderConfiguration bidderConfiguration = new BidderConfiguration();
        bidderConfiguration.id = id;
        return Futures.immediateFuture(new ContextualizedBidResponse(response, bidderConfiguration));
    }

    @Override
    public long getId() {
        return this.id;
    }
}