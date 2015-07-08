package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.biddingservice.Constants;

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
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getIntExtra("requesterId", -1) != this.requesterId)
            return; // Message not for me :'(

        final String creativeFile = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);
        final String notificationUrl = intent.getStringExtra(Constants.NOTIFICATION_URL_ARG);

        webView.setVisibility(View.INVISIBLE);

        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                final String creativeFilePath = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);

                super.onPageFinished(view, url);

                if (notificationUrl != null)
                    notificator.notify(notificationUrl);

                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (webView.getHeight() > 0) {
                            view.setVisibility(View.VISIBLE);
                        } else {
                            creativeFilePath.equals(creativeFilePath);
                            Toast.makeText(context, "Invalid ad, not showing", Toast.LENGTH_LONG).show();
                        }
                    }
                }, 1000);
            }
        });

        webView.loadUrl("file://" + creativeFile);
    }
}
