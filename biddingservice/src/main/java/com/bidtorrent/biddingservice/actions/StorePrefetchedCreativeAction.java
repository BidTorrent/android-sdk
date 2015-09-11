package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

import java.util.Date;

public class StorePrefetchedCreativeAction implements ServiceAction {
    private PrefetchAdsPool prefetchedAdsPool;
    private PublisherConfiguration publisherConfiguration;

    public StorePrefetchedCreativeAction(PrefetchAdsPool prefetchedAdsPool,
                                         PublisherConfiguration publisherConfiguration) {
        this.prefetchedAdsPool = prefetchedAdsPool;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void handleIntent(Intent intent) {
        Imp impression;
        String impressionId;

        impressionId = ActionHelper.getImpressionId(intent);
        String creativeFilePath = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);
        impression = this.publisherConfiguration.getImpressionById(impressionId);

        // FIXME expirationDate from where?
        long expirationDate = intent.getLongExtra(Constants.PREFETCHED_CREATIVE_EXPIRATION_ARG, System.currentTimeMillis());
        long auctionId = intent.getLongExtra(Constants.AUCTION_ID_ARG, -1);

        this.prefetchedAdsPool.addPrefetchedItem(
            impression,
            auctionId,
            creativeFilePath,
            new Date(expirationDate + 10000000));
    }
}
