package com.bidtorrent.bidding;

import java.util.concurrent.Callable;

public interface IBidder {
    Callable<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback);

    long getId();
}
