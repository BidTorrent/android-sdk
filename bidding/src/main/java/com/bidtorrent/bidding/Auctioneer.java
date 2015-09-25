package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.ContextualizedBidResponse;
import com.bidtorrent.bidding.messages.Imp;
import com.google.common.base.Function;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import javax.annotation.Nullable;

public class Auctioneer implements IAuctioneer {
    public final long timeout;
    private final ExecutorService executor;

    public Auctioneer(long timeout, ExecutorService executor) {
        this.timeout = timeout;
        this.executor = executor;
    }

    @Override
    public Future<AuctionResult> runAuction(final Auction auction) {
        Future<AuctionResult> resultFuture;

        resultFuture = this.executor.submit(new Callable<AuctionResult>() {
            @Override
            public AuctionResult call() throws ExecutionException, InterruptedException {
                Collection<ListenableFuture<ContextualizedBidResponse>> responseFutures;
                ListenableFuture<List<ContextualizedBidResponse>> responses;

                responseFutures = pushResponseFutures(auction.getImpression(), auction.getBidders());
                responses = getBidResponses(responseFutures);

                return Futures.lazyTransform(responses, new Function<List<ContextualizedBidResponse>, AuctionResult>() {
                    @Nullable
                    @Override
                    public AuctionResult apply(List<ContextualizedBidResponse> input) {
                        input.removeAll(Collections.singleton(null));
                        return buildAuctionResult(input, auction.getImpression().bidfloor);
                    }
                }).get();
            }
        });

        return resultFuture;
    }

    private static AuctionResult buildAuctionResult(Collection<ContextualizedBidResponse> responses, float floor) {
        float secondPrice;
        ContextualizedBidResponse runnerUp = null;
        ContextualizedBidResponse winningBid;
        SortedSet<ContextualizedBidResponse> sortedResponses;
        Iterator<ContextualizedBidResponse> iterator;

        sortedResponses = new TreeSet<>(responses);
        iterator = sortedResponses.iterator();

        if (!iterator.hasNext())
            return new AuctionResult();

        winningBid = iterator.next();

        if (iterator.hasNext()){
            ContextualizedBidResponse second = iterator.next();
            runnerUp = second;
            secondPrice = runnerUp.getBidResponse().getPrice();
        } else
            secondPrice = floor;

        return new AuctionResult(winningBid, sortedResponses, runnerUp, secondPrice);
    }

    private static Collection<ListenableFuture<ContextualizedBidResponse>> pushResponseFutures(Imp impression, List<IBidder> bidders) {
        Collection<ListenableFuture<ContextualizedBidResponse>> responseFutures;

        responseFutures = new ArrayList<>(bidders.size());

        for (final IBidder bidder: bidders)
            responseFutures.add(bidder.bid(impression, errorCallback));

        return responseFutures;
    }

    private static IErrorCallback errorCallback =  new IErrorCallback() {
        @Override
        public void processError(Exception e) {
            e.printStackTrace();
        }
    };

    private static ListenableFuture<List<ContextualizedBidResponse>> getBidResponses(Collection<ListenableFuture<ContextualizedBidResponse>> responseFutures) {
        return Futures.successfulAsList(responseFutures);
    }
}
