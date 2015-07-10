package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

import java.util.Date;

public class StorePrefetchedCreativeAction implements ServiceAction {
    private PrefetchAdsPool prefetchedAdsPool;

    public StorePrefetchedCreativeAction(PrefetchAdsPool prefetchedAdsPool) {
        this.prefetchedAdsPool = prefetchedAdsPool;
    }

    @Override
    public void handleIntent(Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = ActionHelper.getBidOpportunity(intent);
        String creativeFilePath = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);

        // FIXME expirationDate from where?
        long expirationDate = intent.getLongExtra(Constants.PREFETCHED_CREATIVE_EXPIRATION_ARG, System.currentTimeMillis());
        long auctionId = intent.getLongExtra(Constants.AUCTION_ID_ARG, -1);

        this.prefetchedAdsPool.addPrefetchedItem(
                bidOpportunity,
                auctionId,
                creativeFilePath,
                new Date(expirationDate + 10000000));
    }
}
