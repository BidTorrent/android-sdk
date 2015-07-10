package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.Constants;

public class NotificationsAction implements ServiceAction{

    private Notificator notificator;
    private PublisherConfiguration publisherConfiguration;

    public NotificationsAction(Notificator notificator, PublisherConfiguration publisherConfiguration) {
        this.notificator = notificator;
        this.publisherConfiguration = publisherConfiguration;
    }

    @Override
    public void handleIntent(Intent intent) {
        AuctionResult result = (AuctionResult) intent.getSerializableExtra(Constants.AUCTION_RESULT_ARG);
        BidOpportunity opp = ActionHelper.getBidOpportunity(intent);

        String notificationUrl = result.getWinningBid().buildNotificationUrl(result.getWinningBid().id, "", result.getRunnerUp());
        this.notificator.notify(notificationUrl);

        try {
            String loggingUrl = this.getLoggingUrl(result, opp);
            this.notificator.notify(loggingUrl);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private String getLoggingUrl(AuctionResult result, BidOpportunity opp) {
        StringBuilder builder = new StringBuilder();
        String a = "";
        builder.append("log.bidtorrent.io/imp?");

        for (BidResponse response : result.getResponses()){
            a = response.id + "-" + response.seatbid.get(0).bid.get(0).impid;
            //FIXME: We need to send the entire bidder
            builder.append(String.format("d[%s]=%.2f-%s&", response.id, response.getPrice(), response.getBidderId()));
        }

        builder.append(String.format("f=%.2f&", publisherConfiguration.imp.get(0).bidfloor));
        builder.append(String.format("p=%s&", publisherConfiguration.site.publisher.id));
        builder.append(String.format("a=%s", a));

        return builder.toString();

    }
}
