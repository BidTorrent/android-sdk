package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.biddingservice.Constants;

import java.io.File;

public class CreativeDisplayReceiver extends BroadcastReceiver {
    private final int requesterId;
    private WebView webView;
    private Notificator notificator;

    public CreativeDisplayReceiver(WebView webView, int requesterId, Notificator notificator) {
        this.webView = webView;
        this.requesterId = requesterId;
        this.notificator = notificator;

        this.webView.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getIntExtra("requesterId", -1) != this.requesterId)
            return; // Message not for me :'(

        final String creativeFile = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);
        final String notificationUrl = intent.getStringExtra(Constants.NOTIFICATION_URL_ARG);

        webView.setVisibility(View.INVISIBLE);
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.setVisibility(View.VISIBLE);
                new File(creativeFile).delete();

                if (notificationUrl != null)
                    notificator.notify(notificationUrl);
            }
        });
        webView.loadUrl("file://" + creativeFile);
    }
}
