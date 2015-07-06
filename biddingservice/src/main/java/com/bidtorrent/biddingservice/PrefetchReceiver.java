package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class PrefetchReceiver extends BroadcastReceiver {
    public PrefetchReceiver() {
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle extras = intent.getExtras();
        String creative = extras.getString(BiddingIntentService.CREATIVE_CODE_ARG);
        final WebView webView = new WebView(context);

        webView.loadData(creative, "text/html", "utf-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                view.saveWebArchive(context.getCacheDir().getAbsolutePath(),
                        true, new ValueCallback<String>() {
                            @Override
                            public void onReceiveValue(String value) {
                                Intent auctionIntent;
                                auctionIntent = new Intent(context, BiddingIntentService.class);
                                auctionIntent.setAction(BiddingIntentService.FILL_PREFETCH_BUFFER_ACTION);
                                auctionIntent.putExtra(BiddingIntentService.PREFETCHED_CREATIVE_FILE_ARG, value);
                                auctionIntent.putExtra(BiddingIntentService.BID_OPPORTUNITY_ARG,
                                        intent.getStringExtra(BiddingIntentService.BID_OPPORTUNITY_ARG));

                                context.startService(auctionIntent);
                                view.destroy();
                            }
                        });
            }
        });
    }
}
