package com.bidtorrent.bidding;

import com.google.common.io.ByteStreams;

import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ClientConnectionManager;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.net.URI;
import java.util.concurrent.Callable;

public class HttpBidder implements IBidder{
    private long id;
    private final String name;
    private final URI bidUri;
    private final IResponseConverter<String> responseConverter;
    private ClientConnectionManager connectionManager;
    private HttpParams httpParameters;

    public HttpBidder(long id, String name, URI bidUri, IResponseConverter<String> responseConverter, int timeout)
    {
        this.id = id;
        this.name = name;
        this.bidUri = bidUri;
        this.responseConverter = responseConverter;
        this.httpParameters = new BasicHttpParams();

        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, timeout);
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        this.connectionManager = new ThreadSafeClientConnManager(httpParameters, schemeRegistry);
    }

    @Override
    public Callable<BidResponse> bid(BidOpportunity opportunity, IErrorCallback errorCallback) {
        return new Callable<BidResponse>() {
            @Override
            public BidResponse call() {
                final HttpGet httpGet = new HttpGet(bidUri);
                final DefaultHttpClient httpClient;

                httpClient = new DefaultHttpClient(connectionManager, httpParameters);
                org.apache.http.HttpResponse response = null;
                try {
                    response = httpClient.execute(httpGet);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                if (response != null && response.getEntity() != null) {
                    try {
                        return responseConverter.convert(new String(ByteStreams.toByteArray(response.getEntity().getContent())), id);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                return null;
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
