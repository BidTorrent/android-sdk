package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.ContextualizedBidResponse;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.Serializable;

public interface IBidder extends Serializable{
    ListenableFuture<ContextualizedBidResponse> bid(Imp impression, IErrorCallback errorCallback);
    long getId();
}
