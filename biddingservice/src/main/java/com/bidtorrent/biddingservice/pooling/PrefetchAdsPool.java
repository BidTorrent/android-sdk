package com.bidtorrent.biddingservice.pooling;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.messages.Imp;
import com.google.common.base.Function;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class PrefetchAdsPool {
    public static final int PREFETCH_EXPIRATION_TIME_LIMIT = 10 * 1000;
    private final Map<Imp, Map<Long, ExpiringItem>> waitingForBids;
    private final Map<Imp, Map<Long, PendingPrefetchItem>> waitingForPrefetch;
    private final Map<Imp, Collection<ReadyAd>> readyAds;
    private final Map<Imp, TreeSet<WaitingClient>> waitingClients;
    private final Map<Imp, Object> opportunityLocks;

    private final PoolSizer pool;
    private final MyTwoParametersFunction<WaitingClient, ReadyAd> triggerClientDisplay;
    private final Function<WaitingClient, Boolean> triggerClientPassback;
    private final Function<Imp, ListenableFuture<AuctionResult>> triggerBid;
    private final MyThreeParametersFunction<Imp, AuctionResult, Long> triggerPrefetch;
    private final AtomicLong currentAuctionId;
    private long clientExpirationInMillis;
    private AtomicBoolean hasConnection;

    public PrefetchAdsPool(
            PoolSizer pool,
            MyTwoParametersFunction<WaitingClient, ReadyAd> triggerClientDisplay,
            Function<Imp, ListenableFuture<AuctionResult>> triggerBid,
            MyThreeParametersFunction<Imp, AuctionResult, Long> triggerPrefetch,
            long clientExpirationInMillis,
            boolean connectionAvailable, Function<WaitingClient, Boolean> triggerClientPassback) {
        this.pool = pool;
        this.triggerClientDisplay = triggerClientDisplay;
        this.triggerBid = triggerBid;
        this.triggerPrefetch = triggerPrefetch;
        this.clientExpirationInMillis = clientExpirationInMillis;
        this.triggerClientPassback = triggerClientPassback;
        this.waitingForBids = new HashMap<>();
        this.waitingForPrefetch = new HashMap<>();
        this.readyAds = new HashMap<>();
        this.opportunityLocks = new HashMap<>();
        this.waitingClients = new HashMap<>();
        this.currentAuctionId = new AtomicLong(0);
        this.hasConnection = new AtomicBoolean(connectionAvailable);
    }

    public void addWaitingClient(Imp impression, int clientId)
    {
        synchronized (opportunityLocks) {
            if (!opportunityLocks.containsKey(impression))
                opportunityLocks.put(impression, new Object());
        }

        synchronized (opportunityLocks.get(impression)){
            if (!this.waitingForBids.containsKey(impression))
                this.waitingForBids.put(impression, new HashMap<Long, ExpiringItem>());

            if (!this.waitingForPrefetch.containsKey(impression))
                this.waitingForPrefetch.put(impression, new HashMap<Long, PendingPrefetchItem>());

            if (!this.readyAds.containsKey(impression)){
                //FIXME: Treeset?
                this.readyAds.put(impression, new TreeSet<ReadyAd>());
            }

            if (!this.waitingClients.containsKey(impression)){
                this.waitingClients.put(impression, new TreeSet<WaitingClient>());
            }

            this.waitingClients.get(impression).add(new WaitingClient(
                new Date(System.currentTimeMillis() + this.clientExpirationInMillis),
                clientId, triggerClientPassback));
        }

        refreshBuckets(impression);
    }

    public void updateConnectionStatus(boolean hasConnection){
        this.hasConnection.set(hasConnection);
    }

    public void addPrefetchedItem(Imp impression, long auctionId, String fileName, Date expirationDate){
        synchronized (opportunityLocks.get(impression)){
            PendingPrefetchItem pendingPrefetchItem = this.waitingForPrefetch.get(impression).remove(auctionId);
            if (this.hasConnection.get() && pendingPrefetchItem != null){
                this.readyAds.get(impression).add(new ReadyAd(pendingPrefetchItem.getResult(), fileName, expirationDate));
            }
        }

        this.refreshBuckets(impression); //New data available
    }

    public void refreshBuckets()
    {
        if (!this.hasConnection.get()) return;

        for (Imp impression: opportunityLocks.keySet()) {
            refreshBuckets(impression);
        }
    }

    private void refreshBuckets(Imp impression) {
        removeExpiredItems(impression);
        feedWaitingClients(impression);
        refillBidBucket(impression);
        runAuctions(impression);
    }

    public void discardQueuedItems() {
        for (Imp impression: opportunityLocks.keySet()) {
            discardQueuedItems(impression);
        }
    }

    private void discardQueuedItems(Imp impression) {
        synchronized (opportunityLocks.get(impression)) {
            this.waitingForBids.get(impression).clear();
            this.waitingForPrefetch.get(impression).clear();
        }
    }

    private void runAuctions(final Imp impression){
        synchronized (opportunityLocks.get(impression)) {
            for (final Map.Entry<Long, ExpiringItem> waitingBid : this.waitingForBids.get(impression).entrySet()) {
                ListenableFuture<AuctionResult> future = this.triggerBid.apply(impression);

                if (future == null) continue;

                Futures.addCallback(future, new FutureCallback<AuctionResult>() {
                    @Override
                    public void onSuccess(AuctionResult result) {
                        //Should not take more than 10 secs to prefetch, then discard
                        Date prefetchExpirationDate = new Date(System.currentTimeMillis() + PREFETCH_EXPIRATION_TIME_LIMIT);
                        onAuctionResultReady(result, impression, waitingBid.getKey(), prefetchExpirationDate);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        synchronized (opportunityLocks.get(impression)) {
                            waitingForBids.get(impression).remove(waitingBid.getKey());
                        }
                    }
                });
            }
        }
    }

    private void refillBidBucket(final Imp impression) {
        synchronized (opportunityLocks.get(impression)) {
            System.out.println("************** Already: " +
                    readyAds.get(impression).size() +
                    this.waitingForPrefetch.get(impression).size() +
                    this.waitingForBids.get(impression).size());

            System.out.println("************** ToHave: " +
                    waitingClients.get(impression).size() +
                    pool.getPoolSize());

            while (this.readyAds.get(impression).size() +
                this.waitingForPrefetch.get(impression).size() +
                this.waitingForBids.get(impression).size() < waitingClients.get(impression).size() + pool.getPoolSize())
            {
                final long currentId = this.currentAuctionId.incrementAndGet();
                final Date expirationDate = new Date(System.currentTimeMillis() + 100000); //FIXME

                this.waitingForBids.get(impression).put(currentId, new ExpiringItem(expirationDate));
            }
        }
    }

    private void onAuctionResultReady(AuctionResult input, Imp impression, long currentId, Date expirationDate) {
        synchronized (opportunityLocks.get(impression)){
            if (waitingForBids.get(impression).remove(currentId) != null){
                if (this.hasConnection.get()){
                    waitingForPrefetch.get(impression).put(currentId, new PendingPrefetchItem(input, expirationDate));
                    this.triggerPrefetch.apply(impression, input, currentId);
                }
            }
        }
    }

    private void feedWaitingClients(Imp impression) {
        synchronized (opportunityLocks.get(impression))
        {
            TreeSet<WaitingClient> waitingClients = this.waitingClients.get(impression);
            Iterator<ReadyAd> readyAdIterator = this.readyAds.get(impression).iterator();

            while (readyAdIterator.hasNext())
            {
                Iterator<WaitingClient> nextClientIt = waitingClients.iterator();

                if (!nextClientIt.hasNext())
                    break;

                this.triggerClientDisplay.apply(nextClientIt.next(), readyAdIterator.next());
                nextClientIt.remove();
                readyAdIterator.remove();
            }
        }
    }

    public void removeExpiredItems(Imp impression)
    {
        synchronized (opportunityLocks.get(impression))
        {
            removeIfExpired(this.waitingClients.get(impression));
            removeIfExpired(this.waitingForBids.get(impression));
            removeIfExpired(this.waitingForPrefetch.get(impression));
            removeIfExpired(this.readyAds.get(impression));
        }
    }

    private static <T extends ExpiringItem> void removeIfExpired(Map<Long, T> toClean)
    {
        Iterator<Map.Entry<Long, T>> it = toClean.entrySet().iterator();

        while (it.hasNext())
            expireIt(it, it.next().getValue());
    }

    private static <T extends ExpiringItem> void expireIt(Iterator it, T expirable) {
        if (expirable.isExpired()){
            it.remove();
            expirable.expire();
        }
    }

    private static <T extends ExpiringItem> void removeIfExpired(Collection<T> toClean)
    {
        Iterator<T> it = toClean.iterator();

        while (it.hasNext())
            expireIt(it, it.next());
    }

    public void prefetchFailed(Imp impression, long auctionId) {
        synchronized (opportunityLocks.get(impression)){
            this.waitingForPrefetch.get(impression).remove(auctionId);
        }
    }
}
