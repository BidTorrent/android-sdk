package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.Imp;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.Serializable;
import java.util.concurrent.Callable;

public interface IBidder extends Serializable{
    ListenableFuture<BidResponse> bid(Imp impression, IErrorCallback errorCallback);

    long getId();
}
