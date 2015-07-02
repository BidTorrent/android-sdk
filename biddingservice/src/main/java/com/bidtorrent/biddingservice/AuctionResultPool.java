package com.bidtorrent.biddingservice;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.Size;
import com.google.common.base.Function;
import com.google.common.base.Predicate;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
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

    public AuctionResultPool(
            Function<BidOpportunity, Future<AuctionResult>> auctionRunner,
            PoolSizer poolSizer)
    {
        this.auctionRunner = auctionRunner;
        this.poolSizer = poolSizer;
        this.resultsPools = new HashMap<>();
        this.waitingClients = new HashMap<>();
        this.threadPool = Executors.newCachedThreadPool();
    }

    public void getAuctionResult(BidOpportunity bidOpportunity, Predicate<Future<AuctionResult>> cb)
    {
        if (!this.resultsPools.containsKey(bidOpportunity))
            this.resultsPools.put(bidOpportunity, new LinkedBlockingQueue<PoolItem>());

        if (!this.waitingClients.containsKey(bidOpportunity))
            this.waitingClients.put(bidOpportunity, new LinkedBlockingQueue<Predicate<Future<AuctionResult>>>());

        this.waitingClients.get(bidOpportunity).add(cb);
        this.fillPools();
    }

    public void fillPools()
    {
        this.threadPool.submit(new Runnable() {
            @Override
            public void run() {
                int expectedPoolsSize;

                expectedPoolsSize = poolSizer.getPoolSize();
                for (Map.Entry<BidOpportunity, Queue<PoolItem>> poolItem : resultsPools.entrySet()) {
                    while (poolItem.getValue().size() < expectedPoolsSize) {
                        poolItem.getValue().add(new PoolItem(null, auctionRunner.apply(poolItem.getKey())));
                    }
                }

                triggerAuctionAvailableEvent();
            }
        });
    }

    private void triggerAuctionAvailableEvent()
    {
        for (Map.Entry<BidOpportunity, Queue<Predicate<Future<AuctionResult>>>> clientsWithSize: this.waitingClients.entrySet())
        {
            for (Predicate<Future<AuctionResult>> waitingClient: clientsWithSize.getValue())
            {
                waitingClient.apply(this.resultsPools.get(clientsWithSize.getKey()).poll().getResult());
            }
        }
    }

    private class PoolItem {
        private Date bidDate;
        private Future<AuctionResult> result;

        private PoolItem(Date bidDate, Future<AuctionResult> result) {
            this.bidDate = bidDate;
            this.result = result;
        }

        public Future<AuctionResult> getResult() {
            return result;
        }
    }
}
