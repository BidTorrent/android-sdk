package com.bidtorrent.bidding;

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

public class HttpBidder implements IBidder{
    private long id;
    private final String name;
    private final URI bidUri;
    private final IResponseConverter<String> responseConverter;
    private ClientConnectionManager connectionManager;
    private HttpParams httpParameters;
    private Gson gson;


    public HttpBidder(long id, String name, URI bidUri, IResponseConverter<String> responseConverter, int timeout)
    {
        this.id = id;
        this.name = name;
        this.bidUri = bidUri;
        this.responseConverter = responseConverter;
        this.httpParameters = new BasicHttpParams();

        gson = new GsonBuilder().create();

        httpParameters = new BasicHttpParams();
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, timeout);
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        this.connectionManager = new ThreadSafeClientConnManager(httpParameters, schemeRegistry);
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

    public String getBidJson(BidOpportunity bidOpportunity){
        BidRequest bidRequest = createBidRequest(bidOpportunity);
        return gson.toJson(bidRequest);
    }

    @Override
    public Callable<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback) {
        return new Callable<BidResponse>() {
            @Override
            public BidResponse call() {
                final HttpPost httpPost = new HttpPost(bidUri);
                try {
                    StringEntity postJson = new StringEntity(getBidJson(null));
                    postJson.setContentType("application/json");
                    httpPost.setEntity(postJson);
                } catch (UnsupportedEncodingException e) {
                    //TODO: log here
                }


                //FIXME: To remove, no cookie should be sent inapp
                BasicCookieStore cookieStore = new BasicCookieStore();
                BasicClientCookie ids = new BasicClientCookie("Ids", "a%3A1%3A%7Bi%3A42%3Bs%3A36%3A%226024c0db-5907-4d5c-b43f-da5d7d8a3035%22%3B%7D");
                ids.setDomain("bidtorrent.io");
                ids.setPath("/");
                cookieStore.addCookie(ids);

                final DefaultHttpClient httpClient;
                httpClient = new DefaultHttpClient(connectionManager, httpParameters);

                httpClient.setCookieStore(cookieStore);
                org.apache.http.HttpResponse response = null;
                try {
                    response = httpClient.execute(httpPost);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                BidResponse bidResponse = null;

                if (response != null && response.getEntity() != null) {
                    try {
                        bidResponse = responseConverter.convert(new String(ByteStreams.toByteArray(response.getEntity().getContent())), id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return bidResponse;
            }
        };
    }

    public String getName() {
        return name;
    }

    public long getId() {
        return id;
    }
}
