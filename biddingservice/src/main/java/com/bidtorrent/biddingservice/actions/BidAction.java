package com.bidtorrent.biddingservice.actions;

import android.content.Context;
import android.content.Intent;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

public class BidAction implements ServiceAction {

    private Context context;
    private PrefetchAdsPool prefetchedAdsPool;

    public BidAction(Context context, PrefetchAdsPool prefetchedAdsPool) {
        this.context = context;
        this.prefetchedAdsPool = prefetchedAdsPool;
    }

    @Override
    public void handleIntent(Intent intent) {
        BidOpportunity bidOpportunity;
        int requesterId;

        bidOpportunity = ActionHelper.getBidOpportunity(intent);
        requesterId = ActionHelper.getRequesterId(intent);

        if (bidOpportunity == null) {
            notifyFailure(String.format(
                    "Failed to deserialize the request (should be in the %s field of the intent)",
                    Constants.BID_OPPORTUNITY_ARG));
            return;
        }

        prefetchedAdsPool.addWaitingClient(bidOpportunity, requesterId);
    }

    private void notifyFailure(String errorMsg) {
        Intent responseAvailableIntent = new Intent(Constants.AUCTION_FAILED_INTENT);
        responseAvailableIntent.putExtra(Constants.AUCTION_ERROR_REASON_ARG, errorMsg);
        context.sendBroadcast(responseAvailableIntent);
    }
}
