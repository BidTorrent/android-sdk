package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.DataSetObserver;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ArrayAdapter;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.bidtorrent.bidding.AuctionResult;
import com.bidtorrent.bidding.Notificator;
import com.bidtorrent.bidding.messages.BidResponse;
import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.R;
import com.bidtorrent.biddingservice.debug.ListViewAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Objects;

public class CreativeDisplayReceiver extends BroadcastReceiver {
    private ViewGroup debugView;
    private WebView webView;

    public CreativeDisplayReceiver(WebView webView) {
        this(webView, null);
    }

    public CreativeDisplayReceiver(WebView webView, ViewGroup debugView) {
        this.webView = webView;
        this.debugView = debugView;

        this.webView.getSettings().setJavaScriptEnabled(true);
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        if (intent.getIntExtra("requesterId", -1) != this.webView.getId())
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
                            showDebugInfo(context, (AuctionResult) intent.getSerializableExtra(Constants.AUCTION_RESULT_ARG));
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

    private void showDebugInfo(Context context, AuctionResult result) {
        if (this.debugView == null) return;

        ListView listView = new ListView(context);
        listView.setAdapter(new ListViewAdapter(context, R.layout.debug, new ArrayList<BidResponse>(result.getResponses())));
        debugView.addView(listView);
    }

    private static void sendEvent(Context context, Intent intent) {
        Intent auctionIntent;

        auctionIntent = new Intent(context, BiddingIntentService.class)
            .setAction(Constants.NOTIFICATION_ACTION)
            .putExtra(Constants.AUCTION_RESULT_ARG, intent.getSerializableExtra(Constants.AUCTION_RESULT_ARG));

        context.startService(auctionIntent);
    }
}
