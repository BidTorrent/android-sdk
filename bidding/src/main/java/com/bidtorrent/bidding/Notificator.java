package com.bidtorrent.bidding;

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
