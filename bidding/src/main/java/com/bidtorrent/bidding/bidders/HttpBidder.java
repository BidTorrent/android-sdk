package com.bidtorrent.bidding.bidders;

import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.IErrorCallback;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.messages.App;
import com.bidtorrent.bidding.messages.BidRequest;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.Device;
import com.bidtorrent.bidding.messages.Ext;
import com.bidtorrent.bidding.messages.Geo;
import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.Publisher;
import com.bidtorrent.bidding.messages.User;
import com.google.common.util.concurrent.ListenableFuture;

import java.util.Arrays;

public class HttpBidder implements IBidder {

    private long id;
    private final String name;
    private final String bidUrl;
    private final PooledHttpClient pooledHttpClient;

    public HttpBidder(long id, String name, String bidUrl, PooledHttpClient pooledHttpClient)
    {
        this.id = id;
        this.name = name;
        this.bidUrl = bidUrl;
        this.pooledHttpClient = pooledHttpClient;
    }

   @Override
    public ListenableFuture<BidResponse> bid(final Imp impression, IErrorCallback errorCallback) {
        return pooledHttpClient.jsonPost(bidUrl, HttpBidder.createBidRequest(impression), BidResponse.class);
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }

    private static BidRequest createBidRequest(Imp impression){
        return new BidRequest(
                new User("MyID", "MyID"),
                new App("www.bbc.com", new Publisher("123","Yahoo")),
                "EUR",
                new Device(new Geo("USA"), "91.199.242.236", 1, "en", "Apple", "iPhone","iOS","Mozilla/5.0 (iPhone; CPU iPhone OS 6_1_3 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Mobile/10B329"),
                "39f95888-0450-4afc-9b8b-eabd81a69ddc",
                /*Arrays.asList(new Imp(
                        new Banner(Arrays.asList(4), 300, 1, 250),
                        1.75f,
                        "1",
                        1,
                        false)),*/
                Arrays.asList(impression),
                100,
                new Ext(42)
        );
    }


}
