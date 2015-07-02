package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;
import com.google.common.io.ByteStreams;
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

public class PooledHttpClient {

    private ClientConnectionManager connectionManager;
    private HttpParams httpParameters;
    private Gson gson;

    public PooledHttpClient(int timeout) {
        this.gson = new GsonBuilder().create();
        this.httpParameters = new BasicHttpParams();
        // Set the default socket timeout (SO_TIMEOUT)
        // in milliseconds which is the timeout for waiting for data.
        HttpConnectionParams.setSoTimeout(this.httpParameters, timeout);
        SchemeRegistry schemeRegistry = new SchemeRegistry();

        schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
        this.connectionManager = new ThreadSafeClientConnManager(this.httpParameters, schemeRegistry);
    }

    public String doGet(String url){
        final HttpGet httpGet = new HttpGet(url);
        try {
            HttpResponse response = this.getClient().execute(httpGet);

            return parseResponse(response);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
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

    public <T>T jsonGet(String url, Class<T> type){
        this.gson = new GsonBuilder().create();
        String jsonString = doGet(url);
        if (jsonString == null) return null;
        return this.gson.fromJson(jsonString, type);
    }

    public <U,V>V jsonPost(String url, U request, Class<V> responseType){
        final HttpPost httpPost = new HttpPost(url);
        try {
            StringEntity postJson = new StringEntity(gson.toJson(request));
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

        final DefaultHttpClient httpClient = this.getClient();

        httpClient.setCookieStore(cookieStore);
        org.apache.http.HttpResponse response = null;
        try {
            response = httpClient.execute(httpPost);

            String responseJson = parseResponse(response);
            if (responseJson != null)
                return gson.fromJson(responseJson, responseType);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

}
