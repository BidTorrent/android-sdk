package com.bidtorrent.biddingservice;

import android.app.IntentService;
import android.content.Intent;
import android.util.Log;

import com.bidtorrent.bidding.Auction;
import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.BidResponse;
import com.bidtorrent.bidding.BidderConfiguration;
import com.bidtorrent.bidding.BidderConfigurationFilters;
import com.bidtorrent.bidding.BidderSelector;
import com.bidtorrent.bidding.ConstantBidder;
import com.bidtorrent.bidding.HttpBidder;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;
import com.bidtorrent.bidding.PublisherConfiguration;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BiddingIntentService extends IntentService {
    public static String BID_RESPONSE_AVAILABLE = "Manitralalala";
    private final ExecutorService executor;
    private final Auctioneer auctioneer;
    private final BidderSelector selector;
    private final PublisherConfiguration publisherConfiguration;

    public BiddingIntentService() {
        super("BidTorrent bidding service");
        this.executor = Executors.newCachedThreadPool();
        this.auctioneer = new Auctioneer(100000, Executors.newCachedThreadPool());

        //FIXME: Poll real configuration
        this.publisherConfiguration = new PublisherConfiguration(
                new String[0],
                new String[0],
                new String[0],
                new String[0],
                "FR",
                "EUR",
                0.2f,
                1000,
                50000,
                5);

        this.selector = new BidderSelector(publisherConfiguration);

        //FIXME: Poll real bidders
        this.selector.addBidder(
                new BidderConfiguration("http://adlb.me/bidder/bid.php?bidder=pony",
                        new BidderConfigurationFilters(
                                1f,
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>()
                                ),
                        "ssh-rsa ...."));

        this.selector.addBidder(
                new BidderConfiguration("http://adlb.me/bidder/bid.php?bidder=criteo",
                        new BidderConfigurationFilters(
                                1f,
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>(),
                                new ArrayList<String>()
                        ),
                        "ssh-rsa ...."));
    }

    private Future<AuctionResult> runAuction(BidOpportunity bidOpportunity)
    {
        List<IBidder> bidders;

        bidders = new ArrayList<>();

        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.3f, "CREATIVE", "NOTIFYME")));
        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.4f, "CREATIVETHESHIT", "NOTIFYMENOT")));

        for (BidderConfiguration config : this.selector.getAvailableBidders()){
            bidders.add(new HttpBidder(
                    1,
                    "Kitten",
                    URI.create(config.getEndPoint()),
                    new JsonResponseConverter(),
                    this.publisherConfiguration.getSoftTimeout()));
        }

        return this.auctioneer.runAuction(new Auction(bidOpportunity, bidders, 0.5f));
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                Future<AuctionResult> auctionResultFuture;
                AuctionResult auctionResult = null;

                BidOpportunity bidOpportunity = new BidOpportunity(
                        intent.getIntExtra("width", 0),
                        intent.getIntExtra("height", 0),
                        intent.getStringExtra("appName")
                );

                auctionResultFuture = runAuction(bidOpportunity);
                try {
                    auctionResult = auctionResultFuture.get(10000, TimeUnit.MILLISECONDS);
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
        });
    }
}
