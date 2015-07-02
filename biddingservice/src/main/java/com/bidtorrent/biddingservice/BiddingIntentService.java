package com.bidtorrent.biddingservice;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;

import com.bidtorrent.bidding.Auction;
import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.BidderConfiguration;
import com.bidtorrent.bidding.BidderConfigurationFilters;
import com.bidtorrent.bidding.BidderSelector;
import com.bidtorrent.bidding.ConstantBidder;
import com.bidtorrent.bidding.HttpBidder;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.PublisherConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class BiddingIntentService extends LongLivedService {
    public static String AUCTION_RESULT_AVAILABLE = "Manitralalala";
    public static String AUCTION_FAILED = "Manitrololol";
    public static String REQUEST_ARG_NAME = "rq";
    public static String AUCTION_ERROR_REASON_ARG = "reason";

    private ExecutorService executor;
    private Auctioneer auctioneer;
    private AuctionResultPool resultsPool;
    private BidderSelector selector;
    private PublisherConfiguration publisherConfiguration;
    private Notificator notificator;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.executor = Executors.newCachedThreadPool();

        //FIXME: Poll real configuration
        this.publisherConfiguration = new PublisherConfiguration(
                new String[0],
                new String[0],
                new String[0],
                new String[0],
                "FR",
                "EUR",
                0.02f,
                1000,
                50000,
                5);

        this.auctioneer = new Auctioneer(
                publisherConfiguration.getSoftTimeout(),
                Executors.newCachedThreadPool());
        this.notificator = new Notificator(10000);

        this.selector = new BidderSelector(publisherConfiguration);

        //FIXME: Poll real bidders
        this.selector.addBidder(new BidderConfiguration("http://bidder.bidtorrent.io/criteoBid.php",
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

        this.resultsPool = new AuctionResultPool(
                new Function<BidOpportunity, Future<AuctionResult>>() {
                    @Nullable
                    @Override
                    public Future<AuctionResult> apply(@Nullable BidOpportunity bidOpportunity) {
                        List<IBidder> bidders;

                        bidders = new ArrayList<>();

                        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.02f, 1, "", "CREATIVE", "NOTIFYME")));
                        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.03f, 1, "", "CREATIVETHESHIT", "NOTIFYMENOT")));

                        for (BidderConfiguration config : selector.getAvailableBidders()){
                            bidders.add(new HttpBidder(
                                    1,
                                    "Kitten",
                                    URI.create(config.getEndPoint()),
                                    new JsonResponseConverter(),
                                    publisherConfiguration.getSoftTimeout()));
                        }

                        return auctioneer.runAuction(new Auction(bidOpportunity, bidders, 0.02f));
                    }
                },
                new PoolSizer(
                        (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE), 3, 5));
    }

    private void runAuction(BidOpportunity bidOpportunity)
    {
        this.resultsPool.getAuctionResult(bidOpportunity, new Predicate<Future<AuctionResult>>() {
            @Override
            public boolean apply(@Nullable Future<AuctionResult> input) {
                AuctionResult auctionResult;

                try {
                    auctionResult = input.get(10000, TimeUnit.MILLISECONDS);
                    notifySuccess(auctionResult);
                } catch (Exception e) {
                    notifyFailure("Auction reached the timeout w/o returning any bid");
                }

                return true;
            }
        });
    }

    private void notifyFailure(String errorMsg)
    {
        Intent responseAvailableIntent = new Intent(AUCTION_FAILED);

        responseAvailableIntent.putExtra(AUCTION_ERROR_REASON_ARG, errorMsg);

        this.sendBroadcast(responseAvailableIntent);
    }

    private void notifySuccess(AuctionResult auctionResult)
    {
        Intent responseAvailableIntent = new Intent(AUCTION_RESULT_AVAILABLE);

        responseAvailableIntent.putExtra("biddingPrice", auctionResult.getWinningBid().getPrice());
        responseAvailableIntent.putExtra("price", auctionResult.getWinningPrice());

        if (auctionResult.getWinningBid() != null){
            String notificationUrl = auctionResult.getWinningBid().buildNotificationUrl("","",auctionResult.getRunnerUp());
            if (notificationUrl != null)
                this.notificator.notify(notificationUrl);
        }

        this.sendBroadcast(responseAvailableIntent);
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                AuctionResult auctionResult = null;
                BidOpportunity bidOpportunity;
                Gson gson;

                gson = new GsonBuilder().create();
                bidOpportunity = gson.fromJson(intent.getStringExtra(REQUEST_ARG_NAME), BidOpportunity.class);

                if (bidOpportunity == null)
                {
                    notifyFailure(String.format(
                            "Failed to deserialize the request (should be in the %s field of the intent)",
                            REQUEST_ARG_NAME));
                    return;
                }

                runAuction(bidOpportunity);
            }
        });
    }
}
