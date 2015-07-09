package com.bidtorrent.biddingservice.prefetching;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;

import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class JavaScriptReadyListener {
    private final Handler handler;
    private final WebView webView;
    private final Context context;
    private final Intent intent;
    private AtomicBoolean done;

    public JavaScriptReadyListener(Handler handler, WebView webView, Context context, Intent intent) {
        this.handler = handler;
        this.webView = webView;
        this.context = context;
        this.intent = intent;
        this.done = new AtomicBoolean(false);
    }

    @JavascriptInterface
    public void setDone(){
        if (!this.done.getAndSet(true)) {
            this.sendPrefetchedPage();
        }
    }

    private void sendPrefetchedPage() {
        final File tempFile;

        try {
            tempFile = File.createTempFile("bidtorrent", ".mht", context.getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        handler.postDelayed(
                new Runnable() {
                    @Override
                    public void run() {
                        webView.saveWebArchive(tempFile.getAbsolutePath(),
                                false, new ValueCallback<String>() {
                                    @Override
                                    public void onReceiveValue(String value) {
                                        Intent auctionIntent;
                                        auctionIntent = new Intent(context, BiddingIntentService.class);
                                        auctionIntent.setAction(Constants.FILL_PREFETCH_BUFFER_ACTION);
                                        auctionIntent.putExtra(Constants.PREFETCHED_CREATIVE_FILE_ARG, value)
                                                .putExtra(Constants.BID_OPPORTUNITY_ARG,
                                                        intent.getStringExtra(Constants.BID_OPPORTUNITY_ARG))
                                                .putExtra(Constants.NOTIFICATION_URL_ARG,
                                                        intent.getStringExtra(Constants.NOTIFICATION_URL_ARG))
                                                .putExtra(Constants.AUCTION_ID_ARG,
                                                        intent.getLongExtra(Constants.AUCTION_ID_ARG, -1));


                                        context.startService(auctionIntent);
                                        webView.destroy();
                                    }
                                });
                    }
                }, 1000);
    }
}