package com.bidtorrent.bidding;

import com.google.common.base.Function;
import com.google.common.io.ByteStreams;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.ListeningExecutorService;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.common.util.concurrent.SettableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
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
import java.lang.reflect.Type;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;

import javax.annotation.Nullable;

public class PooledHttpClient {

    private final Timer timer;
    private ClientConnectionManager connectionManager;
    private HttpParams httpParameters;
    private Gson gson;
    private ListeningExecutorService executorService;
    private final LinkedBlockingQueue<PendingTask> pendingTasks;
    private boolean isNetworkAvailable;

    public PooledHttpClient(int timeout, boolean isNetworkAvailable) {
        this.isNetworkAvailable = isNetworkAvailable;
        this.gson = new GsonBuilder().create();
        this.httpParameters = new BasicHttpParams();
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(this.httpParameters, timeout);
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        this.connectionManager = new ThreadSafeClientConnManager(this.httpParameters, schemeRegistry);
        this.executorService = MoreExecutors.listeningDecorator(Executors.newCachedThreadPool());
        this.pendingTasks = new LinkedBlockingQueue<>();

        this.timer = new Timer("pooledhttptimer");
        this.timer.schedule(new TimerTask() {
            @Override
            public void run() {
                runPendingTasks();
            }
        }, 0, 500);
    }

    public ListenableFuture<String> doGet(final String url){
        return this.createFutureTask(new Callable<String>() {
            @Override
            public String call() throws Exception {
                HttpGet httpGet = new HttpGet(url);
                HttpResponse response = getClient().execute(httpGet);
                return parseResponse(response);
            }
        });
    }

    private static String parseResponse(HttpResponse response) throws IOException {
        if (response != null && response.getEntity() != null)
            return new String(ByteStreams.toByteArray(response.getEntity().getContent()));
        return null;
    }

    private DefaultHttpClient getClient() {
        final DefaultHttpClient httpClient;

        httpClient = new DefaultHttpClient(this.connectionManager, this.httpParameters);
        return httpClient;
    }

    public <T> ListenableFuture<T> jsonGet(String url, final Class<T> type){
        return Futures.transform(this.doGet(url), new Function<String, T>() {
            @Nullable
            @Override
            public T apply(String input) {
                if (input == null) return null;
                return gson.fromJson(input, type);
            }
        });
    }

    public <T>ListenableFuture<T> jsonGet(final String url, final Type type) {
        return Futures.transform(this.doGet(url), new Function<String, T>() {
            @Nullable
            @Override
            public T apply(String input) {
                if (input == null) return null;
                return gson.fromJson(input, type);
            }
        });
    }

    public <U,V> ListenableFuture<V> jsonPost(final String url, final U request, final Class<V> responseType){
        return this.createFutureTask(new Callable<V>() {
            @Override
            public V call() throws Exception {
                return doPost(url, request, responseType);
            }
        });
    }

    private <U,V>V doPost(String url, U request, Class<V> responseType) throws Exception {
        org.apache.http.HttpResponse response;
        final HttpPost httpPost = new HttpPost(url);

        StringEntity postJson = new StringEntity(gson.toJson(request));
        postJson.setContentType("application/json");
        httpPost.setEntity(postJson);

        //FIXME: To remove, no cookie should be sent inapp
        BasicCookieStore cookieStore = new BasicCookieStore();
        BasicClientCookie ids = new BasicClientCookie("Ids", "a%3A1%3A%7Bi%3A42%3Bs%3A36%3A%226024c0db-5907-4d5c-b43f-da5d7d8a3035%22%3B%7D");
        ids.setDomain("bidtorrent.io");
        ids.setPath("/");
        cookieStore.addCookie(ids);

        final DefaultHttpClient httpClient = this.getClient();
        httpClient.setCookieStore(cookieStore);
        response = httpClient.execute(httpPost);
        String responseJson = parseResponse(response);
        if (responseJson == null)
            return null;

        return gson.fromJson(responseJson, responseType);
    }

    private void runPendingTasks(){
        if (this.isNetworkAvailable) {
            synchronized (this.pendingTasks){
                while (!this.pendingTasks.isEmpty()) {
                    if (isNetworkAvailable) {
                        final PendingTask pendingTask = this.pendingTasks.poll();
                        if (pendingTask != null) {
                            this.executorService.submit(new Runnable() {
                                @Override
                                public void run() {
                                    pendingTask.execute();
                                }
                            });
                        }
                    }
                }
            }
        }
    }


    public void setNetworkAvailable(boolean isNetworkAvailable){
        this.isNetworkAvailable = isNetworkAvailable;
        runPendingTasks();
    }

    private <T> SettableFuture<T> createFutureTask(Callable<T> callable){
        SettableFuture<T> future = SettableFuture.create();
        this.pendingTasks.add(new PendingTask(future, callable));

        //runPendingTasks();
        return future;
    }

    private class PendingTask<T> {
        public static final int INITIAL_RETRIES_COUNT = 3;

        private SettableFuture<T> future;
        private Callable<T> callable;
        private int pendingIntents;

        private PendingTask(SettableFuture<T> future, Callable<T> callable) {
            this.future = future;
            this.callable = callable;
            this.pendingIntents = INITIAL_RETRIES_COUNT;
        }

        public void execute(){
            try {
                T t = this.callable.call();
                this.future.set(t);
            } catch (Exception e){
                --this.pendingIntents;

                if (this.pendingIntents == 0)
                    this.future.setException(e);
                else
                    pendingTasks.add(this);
            }
        }
    }
}
