package com.bidtorrent.biddingservice;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.BidOpportunity;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

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
    private Function<BidOpportunity, Future<AuctionResult>> auctionRunner;
    private PoolSizer poolSizer;
    private Map<BidOpportunity, Queue<PoolItem>> resultsPools;
    private Map<BidOpportunity, Queue<Predicate<Future<AuctionResult>>>> waitingClients;
    private Map<BidOpportunity, Future> currentTasks;

    public AuctionResultPool(
            Function<BidOpportunity, Future<AuctionResult>> auctionRunner,
            PoolSizer poolSizer)
    {
        this.auctionRunner = auctionRunner;
        this.poolSizer = poolSizer;
        this.resultsPools = new HashMap<>();
        this.waitingClients = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
        this.currentTasks = new ConcurrentHashMap<>();
    }

    public void getAuctionResult(BidOpportunity bidOpportunity, Predicate<Future<AuctionResult>> cb)
    {
        synchronized (this.resultsPools) {
            if (!this.resultsPools.containsKey(bidOpportunity))
                this.resultsPools.put(bidOpportunity, new LinkedBlockingQueue<PoolItem>());
        }

        synchronized (this.waitingClients) {
            if (!this.waitingClients.containsKey(bidOpportunity))
                this.waitingClients.put(bidOpportunity, new LinkedBlockingQueue<Predicate<Future<AuctionResult>>>());
        }

        this.waitingClients.get(bidOpportunity).add(cb);
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
        Queue<Predicate<Future<AuctionResult>>> opportunityWaitingClients;

        opportunityWaitingClients = this.waitingClients.get(opportunity);

        synchronized (opportunityWaitingClients) {
            while (!this.waitingClients.get(opportunity).isEmpty()) {
                Predicate<Future<AuctionResult>> waitingClient;
                PoolItem paul;

                do {
                    paul = this.resultsPools.get(opportunity).poll();
                } while (paul != null && paul.isExpired());


                if (paul == null)
                    break;

                waitingClient = this.waitingClients.get(opportunity).poll();
                waitingClient.apply(paul.getResult());
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
}
