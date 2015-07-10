package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.pooling.MyThreeParametersFunction;
import com.bidtorrent.biddingservice.pooling.MyTwoParametersFunction;
import com.bidtorrent.biddingservice.pooling.PoolSizer;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;
import com.bidtorrent.biddingservice.pooling.ReadyAd;
import com.bidtorrent.biddingservice.pooling.WaitingClient;
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
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class BiddingIntentService extends LongLivedService {

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
        this.pooledHttpClient = new PooledHttpClient(1000, isNetworkAvailable());
        this.pendingIntentsTimer = new Timer();

/*        ListenableFuture<PublisherConfiguration> futurePublisherConfiguration = pooledHttpClient.jsonGet(
                "http://www.bidtorrent.io/api/publishers/1",
                PublisherConfiguration.class);
*/
        Type listType = new TypeToken<List<BidderConfiguration>>(){}.getType();
        ListenableFuture<List<BidderConfiguration>> futureBiddersConfiguration = pooledHttpClient.jsonGet(
                "http://www.bidtorrent.io/api/bidders",
                listType);

        //FIXME: go back to online bidders
        ListenableFuture<PublisherConfiguration> futurePublisherConfiguration = Futures.immediateFuture(new PublisherConfiguration());

        ListenableFuture<List<Object>> allConfigurationsFuture = Futures.allAsList(futurePublisherConfiguration, futureBiddersConfiguration);

        Futures.addCallback(allConfigurationsFuture, new FutureCallback<List<Object>>() {
            @Override
            public void onSuccess(List<Object> result) {
                if (result == null || result.size() != 2) {
                    throw new RuntimeException("Not all configurations where loaded");
                }

                initializeWithConfiguration((PublisherConfiguration) result.get(0), (List<BidderConfiguration>) result.get(1));
                configurationLoaded = true;
            }

            @Override
            public void onFailure(Throwable t) {
                Log.e("BiddingIntentService", "Failed to retrieve configuration", t);
            }
        });

        startPoolMonitors();

    }

    private void initializeWithConfiguration(PublisherConfiguration result, List<BidderConfiguration> bidders) {
        this.publisherConfiguration = result;
        this.selector = new BidderSelector(this.publisherConfiguration);

        this.auctioneer = new Auctioneer(
                this.publisherConfiguration.tmax,
                Executors.newCachedThreadPool());

        //FIXME: Poll real bidders
        for (BidderConfiguration bidderConfig : bidders){
            if(this.selector.acceptBidder(publisherConfiguration, bidderConfig))
                this.selector.addBidder(bidderConfig);
        }

        this.prefetchedAdsPool = new PrefetchAdsPool(
                new PoolSizer((ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE), 3, 5),
                new MyTwoParametersFunction<WaitingClient, ReadyAd>() {
                    @Override
                    public void apply(WaitingClient client, ReadyAd ad) {
                        sendReadyToDisplayAd(client, ad);
                    }
                },
                new Function<BidOpportunity, ListenableFuture<AuctionResult>>() {
                    @Override
                    public ListenableFuture<AuctionResult> apply(final BidOpportunity bidOpportunity) {
                        final List<IBidder> bidders;
                        String defaultCreative = "<html><head><meta name=\"viewport\" content=\"initial-scale=1, width=300, user-scalable=no\" /></head><body style=\"padding:0px; margin:0px\"><img width=\"100%\" src=\"http://adlb.me/bidder/cache/criteo_I.jpg_320x250.jpeg\"/></body></html>";

                        bidders = new ArrayList<>();

                        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.02f, 1, "", defaultCreative, "NOTIFYME")));
                        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.03f, 1, "", defaultCreative, "NOTIFYMENOT")));

                        for (BidderConfiguration config : selector.getAvailableBidders()) {
                            bidders.add(new HttpBidder(
                                    1,
                                    "Kitten",
                                    config.bid_ep,
                                    new JsonResponseConverter(),
                                    publisherConfiguration.tmax,
                                    pooledHttpClient));
                        }

                        return executor.submit(new Callable<AuctionResult>() {
                            @Override
                            public AuctionResult call() throws Exception {
                                Future<AuctionResult> auctionResultFuture = auctioneer.runAuction(new Auction(bidOpportunity, bidders, 0.02f));
                                return auctionResultFuture.get(10000, TimeUnit.MILLISECONDS);
                            }
                        });
                    }
                },
                new MyThreeParametersFunction<BidOpportunity, AuctionResult, Long>() {
                    @Override
                    public void apply(BidOpportunity bidOpportunity, AuctionResult auctionResult, Long auctionId) {
                        sendItToPrefetch(auctionResult, bidOpportunity, auctionId);
                    }
                }, 5 * 60 * 1000, this.isNetworkAvailable());
                //Timeouts?                150000, 5 * 60 * 1000);

    }

    @Override
    protected void onHandleIntent(final Intent intent) {
        String action;

        if (intent == null)
            return;

        action = intent.getAction();

        if (action == null)
            return;

        if (this.handleMissingConfiguration(intent)) {
            if (intent.getAction().equals(Constants.BID_ACTION))
                this.bid(intent);
            else if (intent.getAction().equals(Constants.FILL_PREFETCH_BUFFER_ACTION))
                this.storePrefetchedCreative(intent);
        }
    }

    private void bid(final Intent intent)
    {
        BidOpportunity bidOpportunity;
        int requesterId;

        bidOpportunity = getBidOpportunity(intent);
        requesterId = getRequesterId(intent);

        if (bidOpportunity == null) {
            notifyFailure(String.format(
                    "Failed to deserialize the request (should be in the %s field of the intent)",
                    Constants.BID_OPPORTUNITY_ARG));
            return;
        }

        prefetchedAdsPool.addWaitingClient(bidOpportunity, requesterId);
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
                if (configurationLoaded) {
                    while (true) {
                        Intent currentIntent = null;

                        synchronized (pendingIntents) {
                            currentIntent = pendingIntents.poll();
                            if (currentIntent == null) {
                                pendingIntentsTimer.cancel();
                                break;
                            }
                        }

                        me.onHandleIntent(currentIntent);
                    }
                }
            }
        }, 0, 1 * 1000);

        return false;
    }

    private void notifyFailure(String errorMsg)
    {
        Intent responseAvailableIntent = new Intent(Constants.AUCTION_FAILED_INTENT);

        responseAvailableIntent.putExtra(Constants.AUCTION_ERROR_REASON_ARG, errorMsg);

        this.sendBroadcast(responseAvailableIntent);
    }

    private void sendItToPrefetch(AuctionResult auctionResult, BidOpportunity bidOpportunity, Long auctionId)
    {
        Intent responseAvailableIntent = new Intent(Constants.BID_AVAILABLE_INTENT);

        responseAvailableIntent.putExtra(Constants.CREATIVE_CODE_ARG, auctionResult.getWinningBid().seatbid.get(0).bid.get(0).creative);
        responseAvailableIntent.putExtra(Constants.BID_OPPORTUNITY_ARG, gson.toJson(bidOpportunity));
        responseAvailableIntent.putExtra(Constants.AUCTION_ID_ARG, auctionId);

        this.sendBroadcast(responseAvailableIntent);
    }

    private void sendReadyToDisplayAd(WaitingClient client, ReadyAd ad) {
        Intent readyDisplay = new Intent(Constants.READY_TO_DISPLAY_AD_INTENT);
        readyDisplay.putExtra(Constants.REQUESTER_ID_ARG, client.getId())
                .putExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG, ad.getCacheFileName());
                //FIXME: .putExtra(NOTIFICATION_URL_ARG, ad.getResult().getWinningBid().buildNotificationUrl());
        sendBroadcast(readyDisplay);
    }

    private void storePrefetchedCreative(final Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = getBidOpportunity(intent);
        String creativeFilePath = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);

        // FIXME expirationDate from where?
        long expirationDate = intent.getLongExtra(Constants.PREFETCHED_CREATIVE_EXPIRATION_ARG, System.currentTimeMillis());
        long auctionId = intent.getLongExtra(Constants.AUCTION_ID_ARG, -1);

        prefetchedAdsPool.addPrefetchedItem(
                bidOpportunity,
                auctionId,
                creativeFilePath,
                new Date(expirationDate + 10000000));
    }

    private int getRequesterId(Intent intent) {
        return intent.getIntExtra(Constants.REQUESTER_ID_ARG, 0);
    }

    private BidOpportunity getBidOpportunity(Intent intent) {
        BidOpportunity bidOpportunity;

        bidOpportunity = gson.fromJson(intent.getStringExtra(Constants.BID_OPPORTUNITY_ARG), BidOpportunity.class);
        return bidOpportunity;
    }

    private void startPoolMonitors() {
        //This can be done in the pool? With the context maybe...
        this.refreshTimer = new Timer();
        this.refreshTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                if (prefetchedAdsPool != null)
                    prefetchedAdsPool.refreshBuckets();
            }
        }, 0, 10000);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean networkAvailable = isNetworkAvailable();
                pooledHttpClient.setNetworkAvailable(networkAvailable);
                if (prefetchedAdsPool != null){
                    prefetchedAdsPool.updateConnectionStatus(networkAvailable);
                    if(networkAvailable)
                        prefetchedAdsPool.refreshBuckets();
                    else
                        prefetchedAdsPool.discardQueuedItems();
                }
            }
        }, new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION));
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }
}
