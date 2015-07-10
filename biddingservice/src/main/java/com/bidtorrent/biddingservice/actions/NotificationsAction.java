package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.biddingservice.Constants;

public class NotificationsAction implements ServiceAction{

    private Notificator notificator;

    public NotificationsAction(Notificator notificator) {
        this.notificator = notificator;
    }

    @Override
    public void handleIntent(Intent intent) {
        AuctionResult result = (AuctionResult) intent.getSerializableExtra(Constants.AUCTION_RESULT_ARG);

        String notificationUrl = result.getWinningBid().buildNotificationUrl(result.getWinningBid().id, "", result.getRunnerUp());
        this.notificator.notify(notificationUrl);
    }
}
