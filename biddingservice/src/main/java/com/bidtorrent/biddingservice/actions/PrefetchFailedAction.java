package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

public class PrefetchFailedAction implements ServiceAction{
    private PrefetchAdsPool prefetchedAdsPool;

    public PrefetchFailedAction(PrefetchAdsPool prefetchedAdsPool) {
        this.prefetchedAdsPool = prefetchedAdsPool;
    }

    @Override
    public void handleIntent(Intent intent) {
        BidOpportunity bidOpportunity;
        bidOpportunity = ActionHelper.getBidOpportunity(intent);

        long auctionId = intent.getLongExtra(Constants.AUCTION_ID_ARG, -1);
        this.prefetchedAdsPool.prefetchFailed(bidOpportunity, auctionId);
    }
}
