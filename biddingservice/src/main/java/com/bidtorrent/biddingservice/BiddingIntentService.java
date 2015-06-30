package com.bidtorrent.biddingservice;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.bidtorrent.bidding.Auction;
import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.BidResponse;
import com.bidtorrent.bidding.ConstantBidder;
import com.bidtorrent.bidding.HttpBidder;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BiddingIntentService extends IntentService {
    public static String BID_RESPONSE_AVAILABLE = "Manitralalala";

    public BiddingIntentService() {
        super("BidTorrent bidding service");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Auctioneer auctioneer;
        List<IBidder> bidders;
        Future<AuctionResult> auctionResultFuture;

        auctioneer = new Auctioneer(100000);
        bidders = new ArrayList<>();

        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.3f, "CREATIVE", "NOTIFYME")));
        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.4f, "CREATIVETHESHIT", "NOTIFYMENOT")));

        bidders.add(new HttpBidder(1, "Kitten", URI.create("http://adlb.me/bidder/bid.php?bidder=pony"), new JsonResponseConverter(), 50000));
        bidders.add(new HttpBidder(2, "Criteo", URI.create("http://adlb.me/bidder/bid.php?bidder=criteo"), new JsonResponseConverter(), 50000));

        auctionResultFuture = auctioneer.runAuction(new Auction(new BidOpportunity(URI.create("http://perdu.com")), bidders, 0.5f));

        AuctionResult auctionResult = null;
        try {
            auctionResult = auctionResultFuture.get(100000, TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            Log.e("BiddingService", "Bidding service failed", e);
        }

        Intent responseAvailableIntent = new Intent(BID_RESPONSE_AVAILABLE);
        if (auctionResult == null)
            responseAvailableIntent.putExtra("success", false);
        else {
            responseAvailableIntent.putExtra("success", true);
            responseAvailableIntent.putExtra("price", auctionResult.getWinningPrice());
        }

        sendBroadcast(responseAvailableIntent);
    }
}
