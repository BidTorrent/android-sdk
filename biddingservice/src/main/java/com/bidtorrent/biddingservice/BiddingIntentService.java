package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.BidderSelector;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.messages.App;
import com.bidtorrent.bidding.messages.Banner;
import com.bidtorrent.bidding.messages.Ext;
import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.Publisher;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.bidtorrent.biddingservice.actions.ActionFactory;
import com.bidtorrent.biddingservice.configuration.PublisherConfigurationLoader;
import com.bidtorrent.biddingservice.functions.TriggerBidFunction;
import com.bidtorrent.biddingservice.pooling.MyThreeParametersFunction;
import com.bidtorrent.biddingservice.pooling.MyTwoParametersFunction;
import com.bidtorrent.biddingservice.pooling.PoolSizer;
import com.bidtorrent.biddingservice.pooling.PrefetchAdsPool;
import com.bidtorrent.biddingservice.pooling.ReadyAd;
import com.bidtorrent.biddingservice.pooling.WaitingClient;
import com.google.common.reflect.TypeToken;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Executors;

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
    private Notificator notificator;

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
        this.notificator = new Notificator(this.pooledHttpClient);
        this.pendingIntentsTimer = new Timer();

        ListenableFuture<PublisherConfiguration> publisherConfiguration = this.loadPublisherConfiguration();

        Type listType = new TypeToken<List<BidderConfiguration>>(){}.getType();
        
        ListenableFuture<List<BidderConfiguration>> futureBiddersConfiguration = pooledHttpClient.jsonGet(
                "http://www.bidtorrent.io/api/bidders",
                listType);

        ListenableFuture<List<Object>> allConfigurationsFuture = Futures.allAsList(publisherConfiguration, futureBiddersConfiguration);

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

    private ListenableFuture<PublisherConfiguration> loadPublisherConfiguration()
    {
        PublisherConfigurationLoader publisherConfigurationLoader =
                new PublisherConfigurationLoader(
                        "bidtorrent.properties",
                        this.getAssets(),
                        this.pooledHttpClient,
                        "http://www.bidtorrent.io/api/publishers/");//TODO in configuration

        return publisherConfigurationLoader.loadConfiguration();
    }

    private Properties loadProperties() throws IOException {
        InputStream stream = this.getAssets().open("bidtorrent.properties");
        Properties properties = new Properties();
        properties.load(stream);
        return properties;
    }

    private PublisherConfiguration loadBaseConfiguration() {
        PublisherConfiguration configuration = new PublisherConfiguration();

        try {
            Properties properties = this.loadProperties();

            configuration.app = new App();
            configuration.app.cat = Arrays.asList(properties.getProperty("app.cat").split(","));
            configuration.app.domain = properties.getProperty("app.domain");
            configuration.app.publisher = new Publisher(properties.getProperty("app.publisher.id"), properties.getProperty("app.publisher.name"));

            configuration.badv = Arrays.asList(properties.getProperty("badv").split(","));
            configuration.bcat = Arrays.asList(properties.getProperty("bcat").split(","));
            configuration.cur = properties.getProperty("cur");
            configuration.imp = Arrays.asList(
                    new Imp(
                            new Banner(
                                    readBType(properties),
                                    Integer.parseInt(properties.getProperty("imp.banner.h", "-1")),
                                    0,
                                    Integer.parseInt(properties.getProperty("imp.banner.w", "-1"))),
                            Float.parseFloat(properties.getProperty("1.75", "-1")),
                            "",
                            Integer.parseInt(properties.getProperty("imp.instl", "-1")),
                            Boolean.parseBoolean(properties.getProperty("imp.secure", "false"))));

            configuration.tmax = Integer.parseInt(properties.getProperty("tmax", "-1"));
            configuration.ext = new Ext();
            configuration.ext.btid = Integer.parseInt(properties.getProperty("ext.btid", "-1"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        if (configuration.app.publisher.id.equals(""))
            throw new IllegalArgumentException("Publisher Id is mandatory for configuration");

        return configuration;
    }

    private static List<Integer> readBType(Properties properties) {
        List<Integer> list = new ArrayList<>();
        for (String s : properties.getProperty("imp.banner.btype").split(",")){
            list.add(Integer.parseInt(s));
        }
        return list;
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
                new TriggerBidFunction(
                        this.selector,
                        this.publisherConfiguration,
                        this.pooledHttpClient,
                        this.executor,
                        this.auctioneer),
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

        if (this.handleMissingConfiguration(intent))
            new ActionFactory(intent).create(
                    this,
                    this.prefetchedAdsPool,
                    notificator,
                    publisherConfiguration).handleIntent(intent);
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

    private void sendItToPrefetch(AuctionResult auctionResult, BidOpportunity bidOpportunity, Long auctionId)
    {
        Intent responseAvailableIntent = new Intent(Constants.BID_AVAILABLE_INTENT);

        responseAvailableIntent
                .putExtra(Constants.CREATIVE_CODE_ARG, auctionResult.getWinningBid().seatbid.get(0).bid.get(0).creative)
                .putExtra(Constants.BID_OPPORTUNITY_ARG, gson.toJson(bidOpportunity))
                .putExtra(Constants.AUCTION_ID_ARG, auctionId);

        this.sendBroadcast(responseAvailableIntent);
    }

    private void sendReadyToDisplayAd(WaitingClient client, ReadyAd ad) {
        Intent readyDisplay = new Intent(Constants.READY_TO_DISPLAY_AD_INTENT);

        readyDisplay
            .putExtra(Constants.REQUESTER_ID_ARG, client.getId())
            .putExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG, ad.getCacheFileName())
            .putExtra(Constants.AUCTION_RESULT_ARG, ad.getResult());

        sendBroadcast(readyDisplay);
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
