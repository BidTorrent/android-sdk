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
    private final PooledHttpClient pooledHttpClient;
    private final ExecutorService executorService;

    public Notificator(PooledHttpClient pooledHttpClient) {
        this.executorService = Executors.newCachedThreadPool();
        this.pooledHttpClient = pooledHttpClient;
    }

    public void notify(final String notifyUrl){
        pooledHttpClient.doGet(notifyUrl);
    }
}
