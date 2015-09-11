package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.WebView;

import com.bidtorrent.biddingservice.Constants;

public class PassbackDisplayReceiver extends BroadcastReceiver {
    private WebView webView;

    public PassbackDisplayReceiver(WebView webView) {
        this.webView = webView;
        this.webView.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getIntExtra("requesterId", -1) != this.webView.getId())
            return; // Message not for me :'(

        final String fallback = intent.getStringExtra(Constants.PASSBACK_URL_ARG);
        webView.setVisibility(View.INVISIBLE);

        webView.loadUrl(fallback);
        webView.setVisibility(View.VISIBLE);
    }
}
