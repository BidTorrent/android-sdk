package com.bidtorrent.biddingservice.actions;

import android.content.Context;
import android.content.Intent;

import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;

public class ActionFactory {

    private Intent intent;

    public ActionFactory(Intent intent) {
        this.intent = intent;
    }

    public ServiceAction create(Context context, PrefetchAdsPool prefetchedAdsPool){
        switch (this.intent.getAction()){
            case Constants.BID_ACTION:
                return new BidAction(context, prefetchedAdsPool);
            case Constants.FILL_PREFETCH_BUFFER_ACTION:
                return new StorePrefetchedCreativeAction(prefetchedAdsPool);
            case Constants.PREFETCH_FAILED_ACTION:
                return new PrefetchFailedAction(prefetchedAdsPool);

            default:
                return null;
        }
    }
}