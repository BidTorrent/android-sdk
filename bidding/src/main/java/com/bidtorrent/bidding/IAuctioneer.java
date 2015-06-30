package com.bidtorrent.bidding;

import java.util.concurrent.Future;

public interface IAuctioneer {
    public Future<AuctionResult> runAuction(Auction auction);
}
