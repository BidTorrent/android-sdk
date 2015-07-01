package com.bidtorrent.bidding;

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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Notificator {

    private ExecutorService executorService;
    private ClientConnectionManager connectionManager;
    private HttpParams httpParameters;

    public Notificator(int timeout) {
        httpParameters = new BasicHttpParams();
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(httpParameters, timeout);
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        this.connectionManager = new ThreadSafeClientConnManager(httpParameters, schemeRegistry);
        executorService = Executors.newCachedThreadPool();
    }

    public void notify(final String notifyUrl){
        executorService.submit( new Runnable() {
            @Override
            public void run() {
                try {
                    final HttpGet httpGet = new HttpGet(notifyUrl);
                    final DefaultHttpClient httpClient;

                    httpClient = new DefaultHttpClient(connectionManager, httpParameters);
                    httpClient.execute(httpGet);
                } catch (IllegalArgumentException | IllegalStateException iae){
                    //Do nothing, bad URL
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
}
