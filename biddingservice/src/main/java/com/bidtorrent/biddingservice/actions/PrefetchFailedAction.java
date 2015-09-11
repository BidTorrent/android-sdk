package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

public class PrefetchFailedAction implements ServiceAction{
    private PrefetchAdsPool prefetchedAdsPool;
    private PublisherConfiguration publisherConfiguration;

    public PrefetchFailedAction(PrefetchAdsPool prefetchedAdsPool, PublisherConfiguration publisherConfiguration) {
        this.prefetchedAdsPool = prefetchedAdsPool;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void handleIntent(Intent intent) {
        Imp impression;
        String impressionId;

        impressionId = ActionHelper.getImpressionId(intent);
        impression = this.publisherConfiguration.getImpressionById(impressionId);

        long auctionId = intent.getLongExtra(Constants.AUCTION_ID_ARG, -1);
        this.prefetchedAdsPool.prefetchFailed(impression, auctionId);
    }
}
