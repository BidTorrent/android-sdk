package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;

import java.util.Objects;

public class CreativeDisplayReceiver extends BroadcastReceiver {
    private final int requesterId;
    private WebView webView;

    public CreativeDisplayReceiver(WebView webView, int requesterId) {
        this.webView = webView;
        this.requesterId = requesterId;

        this.webView.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getIntExtra("requesterId", -1) != this.requesterId)
            return; // Message not for me :'(

        final String creativeFile = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);

        webView.setVisibility(View.INVISIBLE);
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                final String creativeFilePath = intent.getStringExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG);
                webView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (webView.getHeight() > 0) {
                            webView.setVisibility(View.VISIBLE);

                            sendEvent(context, intent);
                        } else {
                            creativeFilePath.equals(creativeFilePath);
                            Toast.makeText(context, "Invalid ad, not showing", Toast.LENGTH_LONG).show();
                        }
                    }
                }, 100);  //Too sad, no javascript can be injected when displaying files.
            }
        });

        webView.loadUrl("file://" + creativeFile);
        System.out.println(creativeFile);
    }

    private static void sendEvent(Context context, Intent intent) {
        Intent auctionIntent;
        auctionIntent = new Intent(context, BiddingIntentService.class);
        auctionIntent.setAction(Constants.NOTIFICATION_ACTION);
        auctionIntent.putExtra(Constants.AUCTION_RESULT_ARG,
                intent.getSerializableExtra(Constants.AUCTION_RESULT_ARG));

        context.startService(auctionIntent);
    }
}
