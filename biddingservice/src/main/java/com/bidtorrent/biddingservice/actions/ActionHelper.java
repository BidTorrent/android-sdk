package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.biddingservice.Constants;

public final class ActionHelper {

    public static String getImpressionId(Intent intent){
        return intent.getStringExtra(Constants.IMPRESSION_ID_ARG);
    }

    public static int getRequesterId(Intent intent) {
        return intent.getIntExtra(Constants.REQUESTER_ID_ARG, 0);
    }
}
