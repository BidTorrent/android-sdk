package com.bidtorrent.biddingservice.actions;

import android.content.Intent;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.ContextualizedBidResponse;
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
        String impressionId = ActionHelper.getImpressionId(intent);

        String notificationUrl = result.getWinningBid().getBidResponse().buildNotificationUrl(result.getWinningBid().getBidResponse().id, "", result.getRunnerUp().getBidderConfiguration().id);
        this.notificator.notify(notificationUrl);

        try {
            String loggingUrl = this.getLoggingUrl(result, impressionId);
            this.notificator.notify(loggingUrl);
        } catch (NullPointerException e){
            e.printStackTrace();
        }
    }

    private String getLoggingUrl(AuctionResult result, String impressionId) {
        StringBuilder builder = new StringBuilder();
        String a = "";
        builder.append("log.bidtorrent.io/imp?");

        for (ContextualizedBidResponse response : result.getResponses()){
            a = response.getBidResponse().id + "-" + response.getBidResponse().seatbid.get(0).bid.get(0).impid;
            //FIXME: We need to send the entire bidder
            builder.append(String.format("d[%s]=%.2f-%s&", response.getBidResponse().id, response.getBidResponse().getPrice(), response.getBidderConfiguration().key));
        }

        Imp impression = this.publisherConfiguration.getImpressionById(impressionId);
        if (impression != null)
            builder.append(String.format("f=%.2f&", impression.bidfloor));


        builder.append(String.format("p=%s&", publisherConfiguration.app.publisher.id));
        builder.append(String.format("a=%s", a));

        return builder.toString();

    }
}
