package com.bidtorrent.biddingservice;

import com.bidtorrent.bidding.BidOpportunity;
import com.google.common.base.Function;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

public class AuctionResultPool {
    private final ExecutorService threadPool;
    private final Function<BidOpportunity, Boolean> triggerPrefetching;
    private final Function<Display, Boolean> triggerDisplay;
    private final PoolSizer poolSizer;
    private final int maxWaitingTimeMs;
    private final int bidExpirationTimeMs;
    private final Map<BidOpportunity, Queue<WaitingClient>> waitingClients;
    private Map<BidOpportunity, Queue<PrefetchedData>> prefetchedData = new ConcurrentHashMap<>();
    private Map<BidOpportunity, Queue<PoolItem>> prefetching = new ConcurrentHashMap<>();

    public AuctionResultPool(
            Function<BidOpportunity, Boolean> triggerPrefetching,
            Function<Display, Boolean> triggerDisplay, PoolSizer poolSizer,
            int maxWaitingTimeMs,
            int bidExpirationTimeMs)
    {
        this.triggerPrefetching = triggerPrefetching;
        this.triggerDisplay = triggerDisplay;
        this.poolSizer = poolSizer;
        this.maxWaitingTimeMs = maxWaitingTimeMs;
        this.bidExpirationTimeMs = bidExpirationTimeMs;
        this.waitingClients = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void getAuctionResult(BidOpportunity bidOpportunity, int requestId)
    {
        synchronized (this.waitingClients) {
            if (!this.waitingClients.containsKey(bidOpportunity))
                this.waitingClients.put(bidOpportunity, new LinkedBlockingQueue<WaitingClient>());
        }

        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.MILLISECOND, this.maxWaitingTimeMs);

        this.waitingClients.get(bidOpportunity).add(new WaitingClient(cal.getTime(), requestId));
        this.fillPools(bidOpportunity);
        assignPrefetchData(bidOpportunity);
    }

    public void fillPools()
    {
        for (BidOpportunity opp: this.waitingClients.keySet())
            this.fillPools(opp);
    }

    public void addPrefetchedData(BidOpportunity opp, PrefetchedData data)
    {
        this.prefetchedData.get(opp).add(data);
        assignPrefetchData(opp);
    }

    private void fillPools(final BidOpportunity bidOpportunity)
    {
        synchronized (this.prefetchedData) {
            if (!this.prefetchedData.containsKey(bidOpportunity))
                this.prefetchedData.put(bidOpportunity, new LinkedBlockingQueue<PrefetchedData>());
        }

        synchronized (this.prefetching) {
            if (!this.prefetching.containsKey(bidOpportunity))
                this.prefetching.put(bidOpportunity, new LinkedBlockingQueue<PoolItem>());
        }

        this.threadPool.submit(new Runnable() {
            @Override
            public void run() {
                Queue<PoolItem> prefetchingItems;
                Queue<PrefetchedData> prefetchedItems;

                prefetchingItems = prefetching.get(bidOpportunity);
                prefetchedItems = prefetchedData.get(bidOpportunity);

                // FIXME: lock on smthg meaningful
                synchronized (prefetchingItems) {
                    while (prefetchingItems.size() + prefetchedItems.size() < poolSizer.getPoolSize()) {
                        Calendar cal = Calendar.getInstance();

                        cal.add(Calendar.MILLISECOND, bidExpirationTimeMs);
                        triggerPrefetching.apply(bidOpportunity);
                        prefetchingItems.add(new PoolItem(cal.getTime()));
                    }
                }
            }
        });
    }

    private void assignPrefetchData(BidOpportunity opportunity) {
        Queue<WaitingClient> opportunityWaitingClients;

        opportunityWaitingClients = this.waitingClients.get(opportunity);

        synchronized (opportunityWaitingClients) {
            while (!opportunityWaitingClients.isEmpty()) {
                WaitingClient nextClient = null;
                PrefetchedData paul;

                while (!opportunityWaitingClients.isEmpty())
                {
                    nextClient = opportunityWaitingClients.peek();

                    if (nextClient == null || !nextClient.isExpired())
                        break;

                    nextClient = null;
                    opportunityWaitingClients.poll();
                }

                if (nextClient == null)
                    return;

                do {
                    paul = this.prefetchedData.get(opportunity).poll();
                } while (paul != null && paul.isExpired());


                if (paul == null)
                    break;

                opportunityWaitingClients.poll();

                this.triggerDisplay.apply(new Display(nextClient.getId(), paul.getFileName(), paul.getNotificationUrl()));
            }
        }
    }

    private class PoolItem {
        private Date expirationDate;

        private PoolItem(Date expirationDate) {
            this.expirationDate = expirationDate;
        }

        public boolean isExpired(){
            return expirationDate.before(new Date());
        }
    }


}
