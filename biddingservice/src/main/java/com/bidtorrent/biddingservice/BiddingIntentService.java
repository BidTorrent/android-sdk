package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.util.Log;

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
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nullable;

public class BiddingIntentService extends LongLivedService {
    // Activity extras
    public static final String AUCTION_ERROR_REASON_ARG = "reason";
    public static final String CREATIVE_CODE_ARG = "creativeCode";
    public static final String PREFETCHED_CREATIVE_FILE_ARG = "creativeFile";
    public static final String PREFETCHED_CREATIVE_EXPIRATION_ARG = "dontexpireme";
    public static final String REQUESTER_ID_ARG = "requesterId";
    public static final String BID_OPPORTUNITY_ARG = "dsadas";

    // Actions
    public static final String BID_AVAILABLE_INTENT = "Manitralalala";
    public static final String AUCTION_FAILED_INTENT = "Manitrololol";
    public static final String READY_TO_DISPLAY_AD_INTENT = "manitrataratarata";

    public static final String BID_ACTION = "please-bid";
    public static final String FILL_PREFETCH_BUFFER_ACTION = "please-store";

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
                new Function<BidOpportunity, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean apply(@Nullable BidOpportunity bidOpportunity) {
                        List<IBidder> bidders;
                        String defaultCreative = "<html><head><meta name=\"viewport\" content=\"initial-scale=1, width=300, user-scalable=no\" /></head><body style=\"padding:0px; margin:0px\"><img width=\"100%\" src=\"http://adlb.me/bidder/cache/criteo_I.jpg_320x250.jpeg\"/></body></html>";

                        bidders = new ArrayList<>();

                        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.02f, 1, "", defaultCreative, "NOTIFYME")));
                        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.03f, 1, "", defaultCreative, "NOTIFYMENOT")));

                        for (BidderConfiguration config : selector.getAvailableBidders()) {
                            bidders.add(new HttpBidder(
                                    1,
                                    "Kitten",
                                    config.getBid_ep(),
                                    new JsonResponseConverter(),
                                    publisherConfiguration.timeout_soft));
                        }

                        Future<AuctionResult> auctionResultFuture = auctioneer.runAuction(new Auction(bidOpportunity, bidders, 0.02f));
                        AuctionResult result;
                        try {
                            result = auctionResultFuture.get(10000, TimeUnit.MILLISECONDS);
                            sendItToPrefetch(result, bidOpportunity);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }

                        return true;
                    }
                },
                new Function<Display, Boolean>() {
                    @Nullable
                    @Override
                    public Boolean apply(@Nullable Display input) {
                        sendReadyToDisplayAd(input);
                        return true;
                    }
                }, new PoolSizer(
                (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE), 3, 5),
                150000, 5 * 60 * 1000);

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

    @Override
    protected void onHandleIntent(final Intent intent) {
        if (intent.getAction().equals(BiddingIntentService.BID_ACTION))
            this.bid(intent);
        else
        if (intent.getAction().equals(BiddingIntentService.FILL_PREFETCH_BUFFER_ACTION))
            this.storePrefetchedCreative(intent);
    }

    private void runAuction(final BidOpportunity bidOpportunity, final int requesterId)
    {
        this.resultsPool.getAuctionResult(bidOpportunity, requesterId);
    }

    private void notifyFailure(String errorMsg)
    {
        Intent responseAvailableIntent = new Intent(AUCTION_FAILED_INTENT);

        responseAvailableIntent.putExtra(AUCTION_ERROR_REASON_ARG, errorMsg);

        this.sendBroadcast(responseAvailableIntent);
    }

    private void sendItToPrefetch(AuctionResult auctionResult, BidOpportunity bidOpportunity)
    {
        Intent responseAvailableIntent = new Intent(BID_AVAILABLE_INTENT);
        Gson gson;

        gson = new GsonBuilder().create();

        // FIXME
        responseAvailableIntent.putExtra(BiddingIntentService.CREATIVE_CODE_ARG, auctionResult.getWinningBid().seatbid.get(0).bid.get(0).creative);
        responseAvailableIntent.putExtra(BiddingIntentService.BID_OPPORTUNITY_ARG, gson.toJson(bidOpportunity));

        this.sendBroadcast(responseAvailableIntent);
    }

    private void notifyDisplay()
    {
        // FIXME: get this from cache
        AuctionResult auctionResult;

        auctionResult = new AuctionResult(1, null, 0, null, null, 0);
        if (auctionResult.getWinningBid() != null){
            String notificationUrl = auctionResult.getWinningBid().buildNotificationUrl("","",auctionResult.getRunnerUp());
            if (notificationUrl != null)
                this.notificator.notify(notificationUrl);
        }
    }

    private void sendReadyToDisplayAd(Display display) {
        int requesterId = display.getRequesterId();

        Intent readyDisplay = new Intent(READY_TO_DISPLAY_AD_INTENT);
        readyDisplay.putExtra(REQUESTER_ID_ARG, requesterId);
        sendBroadcast(readyDisplay.putExtra(PREFETCHED_CREATIVE_FILE_ARG, display.getFileName()));
    }

    private void storePrefetchedCreative(final Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = getBidOpportunity(intent);
        String creativeFilePath = intent.getStringExtra(PREFETCHED_CREATIVE_FILE_ARG);
        long expirationDate = intent.getLongExtra(PREFETCHED_CREATIVE_EXPIRATION_ARG, System.currentTimeMillis());

        // FIXME
        resultsPool.addPrefetchedData(bidOpportunity, new PrefetchedData(creativeFilePath, new Date(expirationDate + 10000000)));
    }

    private int getRequesterId(Intent intent) {
        return intent.getIntExtra(REQUESTER_ID_ARG, 0);
    }

    private void bid(final Intent intent)
    {
        this.executor.submit(new Runnable() {
            @Override
            public void run() {
                BidOpportunity bidOpportunity;
                int requesterId;
                bidOpportunity = getBidOpportunity(intent);
                requesterId = getRequesterId(intent);

                if (bidOpportunity == null) {
                    notifyFailure(String.format(
                            "Failed to deserialize the request (should be in the %s field of the intent)",
                            BID_OPPORTUNITY_ARG));
                    return;
                }

                runAuction(bidOpportunity, requesterId);
            }
        });
    }

    private BidOpportunity getBidOpportunity(Intent intent) {
        BidOpportunity bidOpportunity;
        Gson gson;

        gson = new GsonBuilder().create();
        bidOpportunity = gson.fromJson(intent.getStringExtra(BID_OPPORTUNITY_ARG), BidOpportunity.class);
        return bidOpportunity;
    }
}
