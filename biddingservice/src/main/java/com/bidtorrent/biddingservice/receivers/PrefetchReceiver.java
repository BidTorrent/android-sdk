package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.webkit.ValueCallback;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;

import java.io.File;

public class PrefetchReceiver extends BroadcastReceiver {
    public PrefetchReceiver() {
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Bundle extras = intent.getExtras();
        String creative = extras.getString(Constants.CREATIVE_CODE_ARG);
        final WebView webView = new WebView(context);

        webView.loadData(creative, "text/html", "utf-8");
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(final WebView view, String url) {
                view.saveWebArchive(context.getCacheDir().getAbsolutePath() + File.separator,
                        true, new ValueCallback<String>() {
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
        });
    }
}
