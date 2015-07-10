package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface IBidder extends Serializable{
    ListenableFuture<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback);

    long getId();
}
