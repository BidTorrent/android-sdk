package com.bidtorrent.bidding.bidders;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.IBidder;
import com.bidtorrent.bidding.IErrorCallback;
import com.bidtorrent.bidding.IResponseConverter;
import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.messages.App;
import com.bidtorrent.bidding.messages.Banner;
import com.bidtorrent.bidding.messages.BidRequest;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.bidding.messages.Device;
import com.bidtorrent.bidding.messages.Ext;
import com.bidtorrent.bidding.messages.Geo;
import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.Publisher;
import com.bidtorrent.bidding.messages.User;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicCookieStore;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.impl.cookie.BasicClientCookie;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.concurrent.Callable;

public class HttpBidder implements IBidder {
    private long id;
    private final String name;
    private final String bidUrl;
    private final PooledHttpClient pooledHttpClient;

    public HttpBidder(long id, String name, String bidUrl, IResponseConverter<String> responseConverter, int timeout, PooledHttpClient pooledHttpClient)
    {
        this.id = id;
        this.name = name;
        this.bidUrl = bidUrl;
        this.pooledHttpClient = pooledHttpClient;
    }

    public static String getLocalIpAddress()
    {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {

        }
        return null;
    }


    private static BidRequest createBidRequest(BidOpportunity bidOpportunity){
        return new BidRequest(
                new User("MyID", "MyID"),
                new App("www.bbc.com",1,new Publisher("123","Yahoo")),
                "EUR",
                new Device(new Geo("USA"), "91.199.242.236", 1, "en", "Apple", "iPhone","iOS","Mozilla/5.0 (iPhone; CPU iPhone OS 6_1_3 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Mobile/10B329"),
                "39f95888-0450-4afc-9b8b-eabd81a69ddc",
                Arrays.asList(new Imp(
                                new Banner(Arrays.asList(4), 300, 1, 250),
                                1.75f,
                                "1",
                                1,
                                false)),
                100,
                new Ext(42)
        );
    }

    @Override
    public ListenableFuture<BidResponse> bid(final BidOpportunity opportunity, IErrorCallback errorCallback) {
        return pooledHttpClient.jsonPost(bidUrl, createBidRequest(opportunity), BidResponse.class);
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }
}
