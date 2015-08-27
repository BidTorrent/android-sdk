package com.bidtorrent.biddingservice.functions;

import com.bidtorrent.bidding.Auction;
import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Auctioneer;
import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.BidderSelector;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.JsonResponseConverter;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.bidders.ConstantBidder;
import com.bidtorrent.bidding.bidders.HttpBidder;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.google.common.base.Function;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

public class TriggerBidFunction implements Function<BidOpportunity, ListenableFuture<AuctionResult>> {

    public static final String DEFAULT_CREATIVE = "<html><head><meta name=\"viewport\" content=\"initial-scale=1, width=300, user-scalable=no\" /></head><body style=\"padding:0px; margin:0px\"><img width=\"100%\" src=\"http://adlb.me/bidder/Ads/kitten_ad-300x250.jpg\"/></body></html>";
    private BidderSelector selector;
    private PublisherConfiguration publisherConfiguration;
    private PooledHttpClient pooledHttpClient;
    private ListeningExecutorService executor;
    private Auctioneer auctioneer;

    public TriggerBidFunction(
            BidderSelector selector,
            PublisherConfiguration publisherConfiguration,
            PooledHttpClient pooledHttpClient,
            ListeningExecutorService executor,
            Auctioneer auctioneer) {
        this.selector = selector;
        this.publisherConfiguration = publisherConfiguration;
        this.pooledHttpClient = pooledHttpClient;
        this.executor = executor;
        this.auctioneer = auctioneer;
    }

    @Override
    public ListenableFuture<AuctionResult> apply(final BidOpportunity bidOpportunity) {
        final List<IBidder> bidders;

        bidders = new ArrayList<>();
        bidders.add(new ConstantBidder(4, new BidResponse(4, 0.02f, 1, "", DEFAULT_CREATIVE, "NOTIFYME")));
        bidders.add(new ConstantBidder(3, new BidResponse(3, 0.03f, 1, "", DEFAULT_CREATIVE, "NOTIFYMENOT")));

        for (BidderConfiguration config : selector.getAvailableBidders()) {
            bidders.add(new HttpBidder(
                    1,
                    "Kitten",
                    config.bid_ep,
                    new JsonResponseConverter(),
                    publisherConfiguration.tmax,
                    pooledHttpClient));
        }

        return executor.submit(new Callable<AuctionResult>() {
            @Override
            public AuctionResult call() throws Exception {
                Future<AuctionResult> auctionResultFuture = auctioneer.runAuction(new Auction(bidOpportunity, bidders, 0.02f));
                return auctionResultFuture.get(10000, TimeUnit.MILLISECONDS);
            }
        });
    }
}
