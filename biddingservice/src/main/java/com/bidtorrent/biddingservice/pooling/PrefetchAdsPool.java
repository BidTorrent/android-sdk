package com.bidtorrent.biddingservice.pooling;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.Display;
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

public class PrefetchAdsPool {
    private final ExecutorService threadPool;
    private final Function<BidOpportunity, Boolean> triggerPrefetching;
    private final Function<Display, Boolean> triggerDisplay;
    private final PoolSizer poolSizer;
    private final int maxWaitingTimeMs;
    private final int bidExpirationTimeMs;
    private final Map<BidOpportunity, Queue<WaitingClient>> waitingClients;
    private Map<BidOpportunity, Queue<PrefetchedData>> prefetchedData = new ConcurrentHashMap<>();
    private Map<BidOpportunity, Queue<PoolItem>> prefetching = new ConcurrentHashMap<>();
    private Map<BidOpportunity, Object> perOpportunityLock;

    public PrefetchAdsPool(
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
        this.perOpportunityLock = new HashMap<>();
    }

    public void triggerPrefetching(BidOpportunity bidOpportunity, int requestId)
    {
        synchronized (this.waitingClients) {
            if (!this.waitingClients.containsKey(bidOpportunity))
                this.waitingClients.put(bidOpportunity, new LinkedBlockingQueue<WaitingClient>());
        }

        synchronized (this.perOpportunityLock) {
            if (!this.perOpportunityLock.containsKey(bidOpportunity))
                this.perOpportunityLock.put(bidOpportunity, new String());
        }

        this.waitingClients.get(bidOpportunity).add(new WaitingClient(this.getClientExpirationDate(), requestId));
        this.fillPools(bidOpportunity);
    }

    private Date getClientExpirationDate()
    {
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.MILLISECOND, this.maxWaitingTimeMs);
        return cal.getTime();
    }

    private Date getBidExpirationDate()
    {
        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.MILLISECOND, this.bidExpirationTimeMs);
        return cal.getTime();
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
                Queue<WaitingClient> opportunityClients;
                Queue<PrefetchedData> prefetchedItems;
                Object opportunityLock;

                prefetchingItems = prefetching.get(bidOpportunity);
                prefetchedItems = prefetchedData.get(bidOpportunity);
                opportunityClients = waitingClients.get(bidOpportunity);
                opportunityLock = perOpportunityLock.get(bidOpportunity);

                synchronized (opportunityLock) {
                    long adsInThePipe = prefetchingItems.size() + prefetchedItems.size();

                    if (adsInThePipe < opportunityClients.size() ||
                        adsInThePipe < poolSizer.getPoolSize())
                    {
                        triggerPrefetching.apply(bidOpportunity);
                        prefetchingItems.add(new PoolItem(getBidExpirationDate()));
                        assignPrefetchData(bidOpportunity);
                    }
                }

                assignPrefetchData(bidOpportunity);
            }
        });
    }

    private void assignPrefetchData(BidOpportunity opportunity) {
        Queue<WaitingClient> opportunityClients;
        Object opportunityLock;

        opportunityLock = perOpportunityLock.get(opportunity);
        opportunityClients = this.waitingClients.get(opportunity);

        synchronized (opportunityLock) {
            while (!opportunityClients.isEmpty()) {
                WaitingClient nextClient = null;
                PrefetchedData paul;

                while (!opportunityClients.isEmpty())
                {
                    nextClient = opportunityClients.peek();

                    if (nextClient == null || !nextClient.isExpired())
                        break;

                    nextClient = null;
                    opportunityClients.poll();
                }

                if (nextClient == null)
                    return;

                do {
                    paul = this.prefetchedData.get(opportunity).poll();
                } while (paul != null && paul.isExpired());


                if (paul == null)
                    break;

                opportunityClients.poll();

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
