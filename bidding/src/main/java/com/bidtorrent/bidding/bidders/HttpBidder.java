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

    private static String userAgent;
    private static String ip;
    private static String language;
    private static String model;
    private static String brand;
    private static String country;

    public HttpBidder(long id, String name, String bidUrl, PooledHttpClient pooledHttpClient)
    {
        this.id = id;
        this.name = name;
        this.bidUrl = bidUrl;
        this.pooledHttpClient = pooledHttpClient;
    }

    public static void InitDevice(String userAgent, String ipAddress, String language, String model, String brand)
    {
        HttpBidder.userAgent = userAgent;
        HttpBidder.ip = ipAddress;
        HttpBidder.language = language;
        HttpBidder.model = model;
        HttpBidder.brand = brand;
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

    private static Device MakeCurrentDevice()
    {
        return new Device(
            new Geo(""),//let's use the IP
            HttpBidder.ip,
            1,
            HttpBidder.language,
            HttpBidder.brand,
            HttpBidder.model,
            "Android",
            HttpBidder.userAgent);
    }

    private static BidRequest createBidRequest(Imp impression){
        return new BidRequest(
                new User("MyID", "MyID"),
                new App("www.bbc.com", new Publisher("123","Yahoo")),
                "EUR",
                HttpBidder.MakeCurrentDevice(),
                "39f95888-0450-4afc-9b8b-eabd81a69ddc",
                Arrays.asList(impression),
                100,
                new Ext(42)
        );
    }


}
