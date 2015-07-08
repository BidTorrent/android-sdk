package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrefetchReceiver extends BroadcastReceiver {
    private void sendPrefetchedPage(final WebView view, final Context context, final Intent intent)
    {
        final File tempFile;

        try {
            tempFile = File.createTempFile("bidtorrent", ".mht", context.getCacheDir());
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        view.saveWebArchive(tempFile.getAbsolutePath(),
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
                        view.destroy();
                    }
                });
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle extras = intent.getExtras();
        String creative = extras.getString(Constants.CREATIVE_CODE_ARG);
        final WebView webView = new WebView(context);
        final CountDownLatch latch = new CountDownLatch(1);
        final JavaScriptReadyListener jsListener = new JavaScriptReadyListener(latch);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(jsListener, "jslistener");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                view.loadUrl("javascript:window.onLoad=function(data){ jslistener.setDone() }");
                view.loadUrl("javascript:if (/loaded|complete/.test(document.readyState)){ jslistener.setDone() }");

                try {
                    latch.await(10000, TimeUnit.MILLISECONDS);
                    sendPrefetchedPage(view, context, intent);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });
        webView.loadData(creative, "text/html", "utf-8");
    }

    private class JavaScriptReadyListener {
        private CountDownLatch latch;

        private JavaScriptReadyListener(CountDownLatch latch) {
            this.latch = latch;
        }

        @JavascriptInterface
        public void setDone(){
            latch.countDown();
        }
    }
}
