package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CreativeDisplayReceiver extends BroadcastReceiver {
    private final int requesterId;
    private WebView webView;

    public CreativeDisplayReceiver(WebView webView, int requesterId) {
        this.webView = webView;
        this.requesterId = requesterId;

        this.webView.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();

        if (extras.getInt("requesterId") != this.requesterId) return; // Message not for me :'(

        String creativeFile = extras.getString(BiddingIntentService.PREFETCHED_CREATIVE_FILE_ARG);

        webView.setVisibility(View.INVISIBLE);
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.setVisibility(View.VISIBLE);
            }
        });
        webView.loadUrl("file://" + creativeFile);
    }
}
