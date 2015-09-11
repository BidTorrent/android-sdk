package com.bidtorrent.biddingservice.api;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.widget.TextView;

import com.bidtorrent.bidding.BidOpportunity;
import com.bidtorrent.bidding.Size;
import com.bidtorrent.bidding.messages.BidRequest;
import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.receivers.CreativeDisplayReceiver;
import com.bidtorrent.biddingservice.receivers.PassbackDisplayReceiver;
import com.bidtorrent.biddingservice.receivers.PrefetchReceiver;
import com.google.api.client.json.Json;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BidTorrentHandler {

    private WebView adView;
    private Context context;
    private ViewGroup debugLayout;
    private BroadcastReceiver auctionErrorReceiver;
    private BroadcastReceiver displayReceiver;
    private BroadcastReceiver prefetchReceiver;
    private BroadcastReceiver passbackReceiver;

    public static BidTorrentHandler createHandler(Context context, WebView adView, ViewGroup debugLayout){
        return new BidTorrentHandler(context, adView, debugLayout);
    }

    private BidTorrentHandler(Context context, WebView adView, ViewGroup debugLayout) {
        this.context = context;
        this.debugLayout = debugLayout;

        this.auctionErrorReceiver = this.createErrorReceiver();
        this.displayReceiver = new CreativeDisplayReceiver(adView, this.debugLayout);
        this.prefetchReceiver = new PrefetchReceiver(new Handler(context.getMainLooper()));
        this.passbackReceiver = new PassbackDisplayReceiver(adView);

        this.adView = adView;

        this.postCreate();
    }

    private void postCreate(){
        this.context.registerReceiver(this.auctionErrorReceiver, new IntentFilter(Constants.AUCTION_FAILED_INTENT));
        this.context.registerReceiver(this.displayReceiver, new IntentFilter(Constants.READY_TO_DISPLAY_AD_INTENT));
        this.context.registerReceiver(this.prefetchReceiver, new IntentFilter(Constants.BID_AVAILABLE_INTENT));
        this.context.registerReceiver(this.passbackReceiver, new IntentFilter(Constants.DISPLAY_PASSBACK_AD_INTENT));

        this.context.startService(new Intent(context, BiddingIntentService.class));
    }

    public void onDestroy(){
        this.context.unregisterReceiver(this.auctionErrorReceiver);
        this.context.unregisterReceiver(this.displayReceiver);
        this.context.unregisterReceiver(this.prefetchReceiver);
        this.context.unregisterReceiver(this.passbackReceiver);
    }

    public void runAuction(){
        Size adSize;
        Intent auctionIntent;
        BidOpportunity opp;

        adSize = new Size(this.adView.getWidth(), this.adView.getWidth());

        opp = new BidOpportunity(adSize, "bidtorrent.dummy.app");

        this.debugLayout.removeAllViews();

        auctionIntent = new Intent(this.context, BiddingIntentService.class)
            .setAction(Constants.BID_ACTION)
            .putExtra(Constants.REQUESTER_ID_ARG, this.adView.getId())
            .putExtra(Constants.BID_OPPORTUNITY_ARG, new GsonBuilder().create().toJson(opp));

        this.context.startService(auctionIntent);
    }

    @NonNull
    private BroadcastReceiver createErrorReceiver() {
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Bundle extras = intent.getExtras();

                TextView view = new TextView(context);

                view.append(String.format(
                        "The auction failed: %s\n", extras.getString(Constants.AUCTION_ERROR_REASON_ARG)));

                debugLayout.addView(view);
            }
        };
    }

}
