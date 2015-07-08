package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

public class PrefetchReceiver extends BroadcastReceiver {
    private final Handler handler;

    public PrefetchReceiver(Handler handler)
    {
        this.handler = handler;
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle extras = intent.getExtras();
        String creative = extras.getString(Constants.CREATIVE_CODE_ARG);
        final WebView webView = new WebView(context);
        final JavaScriptReadyListener jsListener = new JavaScriptReadyListener(this.handler, webView, context, intent);

        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(jsListener, "jslistener");

        webView.loadData(creative, "text/html", "utf-8");

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.loadUrl("javascript:window.onLoad=function(data){ jslistener.setDone() }");
                view.loadUrl("javascript:if (/loaded|complete/.test(document.readyState)){ jslistener.setDone() }");
            }
        });
    }

    private class JavaScriptReadyListener {
        private final Handler handler;
        private final WebView webView;
        private final Context context;
        private final Intent intent;
        private AtomicBoolean done;

        private JavaScriptReadyListener(Handler handler, WebView webView, Context context, Intent intent) {
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

            handler.post(
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
                    });
        }
    }
}
