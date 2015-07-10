package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.biddingservice.Constants;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public final class ActionHelper {

    private static Gson gson = new GsonBuilder().create();

    public static BidOpportunity getBidOpportunity(Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = gson.fromJson(intent.getStringExtra(Constants.BID_OPPORTUNITY_ARG), BidOpportunity.class);
        return bidOpportunity;
    }

    public static int getRequesterId(Intent intent) {
        return intent.getIntExtra(Constants.REQUESTER_ID_ARG, 0);
    }
}
