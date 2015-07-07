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
import com.bidtorrent.bidding.Display;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.bidtorrent.bidding.BidderSelector;
import com.bidtorrent.bidding.ConstantBidder;
import com.bidtorrent.bidding.HttpBidder;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.pooling.PoolSizer;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;
import com.bidtorrent.biddingservice.pooling.PrefetchedData;
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
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
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

    // Intents
    public static final String BID_AVAILABLE_INTENT = "Manitralalala";
    public static final String AUCTION_FAILED_INTENT = "Manitrololol";
    public static final String READY_TO_DISPLAY_AD_INTENT = "manitrataratarata";

    // Actions
    public static final String BID_ACTION = "please-bid";
    public static final String FILL_PREFETCH_BUFFER_ACTION = "please-store";
    public static final String NOTIFICATION_URL_ARG = "notif";

    private ListeningExecutorService executor;
    private Auctioneer auctioneer;
    private PrefetchAdsPool prefetchedAdsPool;
    private BidderSelector selector;
    private PublisherConfiguration publisherConfiguration;
    private Timer refreshTimer;
    private PooledHttpClient pooledHttpClient;
    private Gson gson;
    private boolean configurationLoaded;
    private Queue<Intent> pendingIntents;
    private Timer pendingIntentsTimer;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        this.pendingIntents = new LinkedList<>();
        this.configurationLoaded = false;
        this.gson = new GsonBuilder().create();
        this.executor = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        this.pooledHttpClient = new PooledHttpClient(1000);
        this.pendingIntentsTimer = new Timer();

        ListenableFuture<PublisherConfiguration> futurePublisherConfiguration = executor.submit(new Callable<PublisherConfiguration>() {
            @Override
            public PublisherConfiguration call() {
                return pooledHttpClient.jsonGet("http://www.mirari.fr/2PAt?a=open", PublisherConfiguration.class);
            }
        });

        ListenableFuture<List<BidderConfiguration>> futureBiddersConfiguration = executor.submit(new Callable<List<BidderConfiguration>>() {
            @Override
            public List<BidderConfiguration> call() {
                Type listType = new TypeToken<List<BidderConfiguration>>(){}.getType();
                return pooledHttpClient.jsonGet("http://www.mirari.fr/sxq2?a=open", listType);
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

                configurationLoaded = true;
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

        this.prefetchedAdsPool = new PrefetchAdsPool(
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
                prefetchedAdsPool.fillPools();
            }
        }, 0, 5 * 60 * 1000);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                prefetchedAdsPool.fillPools();
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        String action = intent.getAction();

        if (action == null)
            return;

        if (this.handleMissingConfiguration(intent)) {
            if (intent.getAction().equals(BiddingIntentService.BID_ACTION))
                this.bid(intent);
            else if (intent.getAction().equals(BiddingIntentService.FILL_PREFETCH_BUFFER_ACTION))
                this.storePrefetchedCreative(intent);
        }
    }

    /**
     * Possibly stop the intent from being handled and hold it until the configuration has been loaded.
     * @param intent The intent to handle.
     * @return Whether the intent should be handled or not.
     */
    private boolean handleMissingConfiguration(final Intent intent)
    {
        final BiddingIntentService me = this;

        if (configurationLoaded)
            return true;

        synchronized (this.pendingIntents) {
            this.pendingIntents.add(intent);

            // Don't run the timer if we are not the first
            if (this.pendingIntents.size() != 1)
                return false;
        }

        this.pendingIntentsTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (configurationLoaded)
                {
                    while (true) {
                        Intent currentIntent = null;

                        synchronized (pendingIntents) {
                            currentIntent = pendingIntents.poll();
                            if (currentIntent == null)
                            {
                                pendingIntentsTimer.cancel();
                                break;
                            }
                        }

                        me.onHandleIntent(currentIntent);
                    }
                }
            }
        }, 1 * 1000);

        return false;
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

        responseAvailableIntent.putExtra(BiddingIntentService.CREATIVE_CODE_ARG, auctionResult.getWinningBid().seatbid.get(0).bid.get(0).creative);
        responseAvailableIntent.putExtra(BiddingIntentService.BID_OPPORTUNITY_ARG, gson.toJson(bidOpportunity));
        responseAvailableIntent.putExtra(BiddingIntentService.NOTIFICATION_URL_ARG,
                auctionResult.getWinningBid().buildNotificationUrl("", "", auctionResult.getRunnerUp()));

        this.sendBroadcast(responseAvailableIntent);
    }

    private void sendReadyToDisplayAd(Display display) {
        Intent readyDisplay = new Intent(READY_TO_DISPLAY_AD_INTENT);
        readyDisplay.putExtra(REQUESTER_ID_ARG, display.getRequesterId())
                .putExtra(PREFETCHED_CREATIVE_FILE_ARG, display.getFileName())
                .putExtra(NOTIFICATION_URL_ARG, display.getNotificationUrl());
        sendBroadcast(readyDisplay);
    }

    private void storePrefetchedCreative(final Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = getBidOpportunity(intent);
        String creativeFilePath = intent.getStringExtra(PREFETCHED_CREATIVE_FILE_ARG);
        long expirationDate = intent.getLongExtra(PREFETCHED_CREATIVE_EXPIRATION_ARG, System.currentTimeMillis());
        String notificationUrl = intent.getStringExtra(NOTIFICATION_URL_ARG);

        // FIXME
        prefetchedAdsPool.addPrefetchedData(bidOpportunity, new PrefetchedData(creativeFilePath, notificationUrl, new Date(expirationDate + 10000000)));
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

                prefetchedAdsPool.triggerPrefetching(bidOpportunity, requesterId);
            }
        });
    }

    private BidOpportunity getBidOpportunity(Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = gson.fromJson(intent.getStringExtra(BID_OPPORTUNITY_ARG), BidOpportunity.class);
        return bidOpportunity;
    }
}
