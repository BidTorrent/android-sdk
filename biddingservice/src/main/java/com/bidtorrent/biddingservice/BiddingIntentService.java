package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;

import com.bidtorrent.bidding.Auction;
import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.bidtorrent.bidding.BidderSelector;
import com.bidtorrent.bidding.ConstantBidder;
import com.bidtorrent.bidding.HttpBidder;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class BiddingIntentService extends LongLivedService {
    public static String AUCTION_RESULT_AVAILABLE = "Manitralalala";
    public static String AUCTION_FAILED = "Manitrololol";
    public static String REQUEST_ARG_NAME = "rq";
    public static String AUCTION_ERROR_REASON_ARG = "reason";

    private ListeningExecutorService executor;
    private Auctioneer auctioneer;
    private AuctionResultPool resultsPool;
    private BidderSelector selector;
    private PublisherConfiguration publisherConfiguration;
    private Notificator notificator;
    private Timer refreshTimer;
    private PooledHttpClient pooledHttpClient;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        this.executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        this.pooledHttpClient = new PooledHttpClient(1000);

        this.notificator = new Notificator(10000, null);

        ListenableFuture<PublisherConfiguration> futurePublisherConfiguration = executor.submit(new Callable<PublisherConfiguration>() {
            @Override
            public PublisherConfiguration call() {
                return pooledHttpClient.jsonGet("http://static.bidtorrent.io/publisher.json", PublisherConfiguration.class);
            }
        });

        ListenableFuture<List<BidderConfiguration>> futureBiddersConfiguration = executor.submit(new Callable<List<BidderConfiguration>>() {
            @Override
            public List<BidderConfiguration> call() {
                Type listType = new TypeToken<List<BidderConfiguration>>(){}.getType();
                return pooledHttpClient.jsonGet("http://static.bidtorrent.io/bidders.json", listType);
            }
        });

        ListenableFuture<List<Object>> allConfigurationsFuture = Futures.allAsList(futurePublisherConfiguration, futureBiddersConfiguration);

        Futures.addCallback(allConfigurationsFuture, new FutureCallback<List<Object>>() {
            @Override
            public void onSuccess(List<Object> result) {
                if (result == null || result.size() != 2) {
                    throw new RuntimeException("Not all configurations where loaded");
                }

                if (result.get(0) instanceof PublisherConfiguration) {
                    initializeWithConfiguration((PublisherConfiguration) result.get(0), (List<BidderConfiguration>) result.get(1));
                } else {
                    initializeWithConfiguration((PublisherConfiguration) result.get(1), (List<BidderConfiguration>) result.get(0));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("BiddingIntentService", "Failed to retrieve configuration", t);
            }
        });
    }

    private void initializeWithConfiguration(PublisherConfiguration result, List<BidderConfiguration> bidders) {
        this.publisherConfiguration = result;
        this.selector = new BidderSelector(this.publisherConfiguration);

        this.auctioneer = new Auctioneer(
                this.publisherConfiguration.timeout_soft,
                Executors.newCachedThreadPool());

        //FIXME: Poll real bidders
        for (BidderConfiguration config : bidders){
            this.selector.addBidder(config);
        }

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
                                    config.getBid_ep(),
                                    new JsonResponseConverter(),
                                    publisherConfiguration.timeout_soft));
                        }

                        return auctioneer.runAuction(new Auction(bidOpportunity, bidders, 0.02f));
                    }
                },
                new PoolSizer(
                        (ConnectivityManager)getSystemService(Context.CONNECTIVITY_SERVICE), 3, 5),
                15000, 5 * 60 * 1000);

        this.refreshTimer = new Timer();
        this.refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                resultsPool.fillPools();
            }
        }, 0, 5 * 60 * 1000);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                resultsPool.fillPools();
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
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
        responseAvailableIntent.putExtra("creative", auctionResult.getWinningBid().seatbid.get(0).bid.get(0).creative);

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

                if (bidOpportunity == null) {
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
