package com.bidtorrent.biddingservice;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.BidOpportunity;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.AbstractMap;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;

public class AuctionResultPool {
    private final ExecutorService threadPool;
    private final Function<BidOpportunity, Future<AuctionResult>> auctionRunner;
    private final PoolSizer poolSizer;
    private final int maxWaitingTimeMs;
    private final Map<BidOpportunity, Queue<PoolItem>> resultsPools;
    private final Map<BidOpportunity, Queue<WaitingClient>> waitingClients;

    /**
     *
     * @param auctionRunner
     * @param poolSizer
     * @param maxWaitingTimeMs Maximum amount of time during which a bid request could be waiting
     *                         in the queue without anything happening.
     */
    public AuctionResultPool(
            Function<BidOpportunity, Future<AuctionResult>> auctionRunner,
            PoolSizer poolSizer,
            int maxWaitingTimeMs)
    {
        this.auctionRunner = auctionRunner;
        this.poolSizer = poolSizer;
        this.maxWaitingTimeMs = maxWaitingTimeMs;
        this.resultsPools = new HashMap<>();
        this.waitingClients = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void getAuctionResult(BidOpportunity bidOpportunity, Predicate<Future<AuctionResult>> cb)
    {
        synchronized (this.resultsPools) {
            if (!this.resultsPools.containsKey(bidOpportunity))
                this.resultsPools.put(bidOpportunity, new LinkedBlockingQueue<PoolItem>());
        }

        synchronized (this.waitingClients) {
            if (!this.waitingClients.containsKey(bidOpportunity))
                this.waitingClients.put(bidOpportunity, new LinkedBlockingQueue<WaitingClient>());
        }

        Calendar cal = Calendar.getInstance();

        cal.add(Calendar.MILLISECOND, this.maxWaitingTimeMs);

        this.waitingClients.get(bidOpportunity).add(new WaitingClient(cal.getTime(), cb));
        this.fillPools(bidOpportunity);
    }

    private void fillPools(final BidOpportunity bidOpportunity)
    {
        this.threadPool.submit(new Runnable() {
            @Override
            public void run() {
                Queue<PoolItem> poolItems;

                poolItems = resultsPools.get(bidOpportunity);
                while (poolItems.size() < poolSizer.getPoolSize()) {
                    Calendar cal = Calendar.getInstance();

                    cal.add(Calendar.DATE, 1);
                    poolItems.add(new PoolItem(cal.getTime(), auctionRunner.apply(bidOpportunity)));
                    triggerAuctionAvailableEvent(bidOpportunity);
                }

                triggerAuctionAvailableEvent(bidOpportunity);
            }
        });
    }

    private void triggerAuctionAvailableEvent(BidOpportunity opportunity) {
        Queue<WaitingClient> opportunityWaitingClients;

        opportunityWaitingClients = this.waitingClients.get(opportunity);

        synchronized (opportunityWaitingClients) {
            while (!opportunityWaitingClients.isEmpty()) {
                WaitingClient nextClient = null;
                PoolItem paul;

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
                    paul = this.resultsPools.get(opportunity).poll();
                } while (paul != null && paul.isExpired());


                if (paul == null)
                    break;

                opportunityWaitingClients.poll();
                nextClient.getCallback().apply(paul.getResult());
            }
        }
    }

    private class PoolItem {
        private Date expirationDate;
        private Future<AuctionResult> result;

        private PoolItem(Date expirationDate, Future<AuctionResult> result) {
            this.expirationDate = expirationDate;
            this.result = result;
        }

        public Future<AuctionResult> getResult() {
            return result;
        }

        public boolean isExpired(){
            return expirationDate.before(new Date());
        }
    }

    private class WaitingClient
    {
        private final Date expirationDate;
        private final Predicate<Future<AuctionResult>> callback;

        private WaitingClient(Date expirationDate, Predicate<Future<AuctionResult>> callback) {
            this.expirationDate = expirationDate;
            this.callback = callback;
        }

        public Date getExpirationDate() {
            return expirationDate;
        }

        public Predicate<Future<AuctionResult>> getCallback() {
            return callback;
        }

        public boolean isExpired(){
            return expirationDate.before(new Date());
        }
    }
}
