package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Callable;

public interface IBidder {
    ListenableFuture<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback);

    long getId();
}
