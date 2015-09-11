package com.bidtorrent.biddingservice.actions;

import android.content.Context;
import android.content.Intent;

import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

public class BidAction implements ServiceAction {

    private Context context;
    private PrefetchAdsPool prefetchedAdsPool;
    private PublisherConfiguration publisherConfiguration;

    public BidAction(Context context, PrefetchAdsPool prefetchedAdsPool, PublisherConfiguration publisherConfiguration) {
        this.context = context;
        this.prefetchedAdsPool = prefetchedAdsPool;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void handleIntent(Intent intent) {
        Imp impression;
        String impressionId;
        int requesterId;

        impressionId = ActionHelper.getImpressionId(intent);
        requesterId = ActionHelper.getRequesterId(intent);

        if (impressionId == "") {
            notifyFailure(
                String.format(
                    "Failed to deserialize the impression Id (should be in the %s field of the intent)",
                    Constants.IMPRESSION_ID_ARG));

            return;
        }

        impression = this.publisherConfiguration.getImpressionById(impressionId);
        if (impression == null)
        {
            notifyFailure(
                String.format("Failed to find impression with the Id=[%s]", impressionId));

            return;
        }

        prefetchedAdsPool.addWaitingClient(impression, requesterId);
    }

    private void notifyFailure(String errorMsg) {
        Intent responseAvailableIntent;

        responseAvailableIntent = new Intent(Constants.AUCTION_FAILED_INTENT);
        responseAvailableIntent.putExtra(Constants.AUCTION_ERROR_REASON_ARG, errorMsg);

        this.context.sendBroadcast(responseAvailableIntent);
    }
}
