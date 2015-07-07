package com.bidtorrent.biddingservice.pooling;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.BidOpportunity;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicLong;

public class PrefetchAdsPool {
    private final Map<BidOpportunity, Map<Long, ExpiringItem>> waitingForBids;
    private final Map<BidOpportunity, Map<Long, PendingPrefetchItem>> waitingForPrefetch;
    private final Map<BidOpportunity, Collection<ReadyAd>> readyAds;

    private final Map<BidOpportunity, TreeSet<WaitingClient>> waitingClients;

    private final Map<BidOpportunity, Object> opportunityLocks;

    private final PoolSizer pool;
    private final MyTwoParametersFunction<WaitingClient, ReadyAd> triggerClientDisplay;
    private final Function<BidOpportunity, ListenableFuture<AuctionResult>> triggerBid;
    private final MyThreeParametersFunction<BidOpportunity, AuctionResult, Long> triggerPrefetch;
    private final AtomicLong currentAuctionId;
    private long clientExpirationInMillis;

    public PrefetchAdsPool(
            PoolSizer pool,
            MyTwoParametersFunction<WaitingClient, ReadyAd> triggerClientDisplay,
            Function<BidOpportunity, ListenableFuture<AuctionResult>> triggerBid,
            MyThreeParametersFunction<BidOpportunity, AuctionResult, Long> triggerPrefetch, long clientExpirationInMillis) {
        this.pool = pool;
        this.triggerClientDisplay = triggerClientDisplay;
        this.triggerBid = triggerBid;
        this.triggerPrefetch = triggerPrefetch;
        this.clientExpirationInMillis = clientExpirationInMillis;
        this.waitingForBids = new HashMap<>();
        this.waitingForPrefetch = new HashMap<>();
        this.readyAds = new HashMap<>();
        this.opportunityLocks = new HashMap<>();
        this.waitingClients = new HashMap<>();
        currentAuctionId = new AtomicLong(0);
    }

    public void addWaitingClient(BidOpportunity opp, int clientId)
    {
        synchronized (opportunityLocks) {
            if (!opportunityLocks.containsKey(opp))
                opportunityLocks.put(opp, new Object());
        }

        synchronized (opportunityLocks.get(opp)){
            if (!this.waitingForBids.containsKey(opp))
                this.waitingForBids.put(opp, new HashMap<Long, ExpiringItem>());

            if (!this.waitingForPrefetch.containsKey(opp))
                this.waitingForPrefetch.put(opp, new HashMap<Long, PendingPrefetchItem>());

            if (!this.readyAds.containsKey(opp)){
                //FIXME: Treeset?
                this.readyAds.put(opp, new TreeSet<ReadyAd>());
            }

            if (!this.waitingClients.containsKey(opp)){
                this.waitingClients.put(opp, new TreeSet<WaitingClient>());
            }

            this.waitingClients.get(opp).add(new WaitingClient(
                new Date(System.currentTimeMillis() + this.clientExpirationInMillis),
                clientId));
        }

        refreshBuckets(opp);
    }

    public void addPrefetchedItem(BidOpportunity opp, long auctionId, String fileName, Date expirationDate){
        synchronized (opportunityLocks.get(opp)){
            PendingPrefetchItem pendingPrefetchItem = this.waitingForPrefetch.get(opp).remove(auctionId);
            if (pendingPrefetchItem != null){
                this.readyAds.get(opp).add(new ReadyAd(pendingPrefetchItem.getResult(), fileName, expirationDate));
            }
        }

        this.refreshBuckets(opp); //New data available
    }

    public void refreshBuckets()
    {
        for (BidOpportunity opp: opportunityLocks.keySet()) {
            refreshBuckets(opp);
        }
    }

    private void refreshBuckets(BidOpportunity opp) {
        removeExpiredItems(opp);
        feedWaitingClients(opp);
        refillBidBucket(opp);
        runAuctions(opp);
    }

    private void runAuctions(final BidOpportunity opp){
        synchronized (opportunityLocks.get(opp)) {
            for (final Map.Entry<Long, ExpiringItem> waitingBid : this.waitingForBids.get(opp).entrySet()) {
                ListenableFuture<AuctionResult> future = this.triggerBid.apply(opp);
                if (future == null) continue;

                Futures.addCallback(future, new FutureCallback<AuctionResult>() {
                    @Override
                    public void onSuccess(AuctionResult result) {
                        onAuctionResultReady(result, opp, waitingBid.getKey(), waitingBid.getValue().expirationDate);
                    }

                    @Override
                    public void onFailure(Throwable t) {
                        t.printStackTrace();
                        synchronized (opportunityLocks.get(opp)) {
                            waitingForBids.get(opp).remove(waitingBid.getKey());
                        }
                    }
                });
            }
        }
    }

    private void refillBidBucket(final BidOpportunity opp) {

        synchronized (opportunityLocks.get(opp)) {
            while (this.readyAds.get(opp).size() + this.waitingForPrefetch.get(opp).size() + this.waitingForBids.get(opp).size()
                    < waitingClients.get(opp).size() + pool.getPoolSize()){
                final long currentId = this.currentAuctionId.incrementAndGet();
                final Date expirationDate = new Date(System.currentTimeMillis() + 100000); //FIXME

                this.waitingForBids.get(opp).put(currentId, new ExpiringItem(expirationDate));
            }
        }
    }

    private void onAuctionResultReady(AuctionResult input, BidOpportunity opp, long currentId, Date expirationDate) {
        synchronized (opportunityLocks.get(opp)){
            if (waitingForBids.get(opp).remove(currentId) != null){
                waitingForPrefetch.get(opp).put(currentId, new PendingPrefetchItem(input, expirationDate));
                this.triggerPrefetch.apply(opp, input, currentId); //Fixme: CurrentID should be sent
            }
        }
    }

    private void feedWaitingClients(BidOpportunity opp) {
        synchronized (opportunityLocks.get(opp))
        {
            TreeSet<WaitingClient> waitingClients = this.waitingClients.get(opp);
            Iterator<ReadyAd> readyAdIterator = this.readyAds.get(opp).iterator();

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

    public void removeExpiredItems(BidOpportunity opp)
    {
        synchronized (opportunityLocks.get(opp))
        {
            removeIfExpired(this.waitingClients.get(opp));
            removeIfExpired(this.waitingForBids.get(opp));
            removeIfExpired(this.waitingForPrefetch.get(opp));
            removeIfExpired(this.readyAds.get(opp));
        }
    }

    private static <T extends ExpiringItem> void removeIfExpired(Map<Long, T> toClean)
    {
        Iterator<Map.Entry<Long, T>> it = toClean.entrySet().iterator();

        while (it.hasNext()) {
            if (it.next().getValue().isExpired())
                it.remove();
        }
    }

    private static <T extends ExpiringItem> void removeIfExpired(Collection<T> toClean)
    {
        Iterator<T> it = toClean.iterator();

        while (it.hasNext())
        {
            if (it.next().isExpired())
                it.remove();
        }
    }
}
