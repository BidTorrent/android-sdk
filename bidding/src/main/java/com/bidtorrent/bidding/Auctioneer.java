package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;
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
                Collection<ListenableFuture<BidResponse>> responseFutures;
                ListenableFuture<List<BidResponse>> responses;

                responseFutures = pushResponseFutures(auction.getOpportunity(), auction.getBidders());
                responses = getBidResponses(responseFutures);

                return Futures.lazyTransform(responses, new Function<List<BidResponse>, AuctionResult>() {
                    @Nullable
                    @Override
                    public AuctionResult apply(List<BidResponse> input) {
                        input.removeAll(Collections.singleton(null));
                        return buildAuctionResult(input, auction.getFloor(), auction.getBidders());
                    }
                }).get();
            }
        });

        return resultFuture;
    }

    private static AuctionResult buildAuctionResult(Collection<BidResponse> responses, float floor, Collection<IBidder> bidders) {
        float secondPrice;
        long runnerUp = 0l;
        BidResponse winningBid;
        SortedSet<BidResponse> sortedResponses;
        Iterator<BidResponse> iterator;

        sortedResponses = new TreeSet<>(responses);
        iterator = sortedResponses.iterator();

        if (!iterator.hasNext())
            return new AuctionResult(null, 0, null, responses, 0);

        winningBid = iterator.next();

        if (iterator.hasNext()){
            BidResponse second = iterator.next();
            secondPrice = second.getPrice();
            runnerUp = second.getBidderId();
        }
        else
            secondPrice = floor;

        return new AuctionResult(winningBid, secondPrice, getBidderById(winningBid.getBidderId(), bidders), sortedResponses, runnerUp);
    }

    private static Collection<ListenableFuture<BidResponse>> pushResponseFutures(
            final BidOpportunity opportunity, final List<IBidder> bidders)
    {
        Collection<ListenableFuture<BidResponse>> responseFutures;

        responseFutures = new ArrayList<>(bidders.size());
        for (final IBidder bidder: bidders) {
            responseFutures.add(bidder.bid(opportunity,
                    new IErrorCallback() {
                        @Override
                        public void processError(Exception e) {
                            e.printStackTrace();
                        }
                    }));
        }

        return responseFutures;
    }

    private static ListenableFuture<List<BidResponse>> getBidResponses(Collection<ListenableFuture<BidResponse>> responseFutures)
    {
        return Futures.successfulAsList(responseFutures);
    }

    private static IBidder getBidderById(long bidderId, Collection<IBidder> bidders) {
        for (IBidder bidder : bidders) {
            if (bidder.getId() == bidderId)
                return bidder;
        }

        return null;
    }
}
