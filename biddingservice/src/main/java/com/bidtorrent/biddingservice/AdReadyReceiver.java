package com.bidtorrent.biddingservice;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class AdReadyReceiver extends BroadcastReceiver {
    private TextView debugView;
    private WebView webView;

    public AdReadyReceiver(TextView debugView, WebView webView) {
        this.debugView = debugView;
        this.webView = webView;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        String creative = extras.getString("creative");

        debugView.append(String.format(Locale.getDefault(), "Price: %.2f\n", extras.getFloat("price")));
        debugView.append(String.format(Locale.getDefault(), "BiddingPrice: %.2f\n", extras.getFloat("biddingPrice")));

        this.webView.getSettings().setJavaScriptEnabled(true);
        this.webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                view.setVisibility(View.VISIBLE);
            }
        });
        webView.loadData(creative, "text/html", "UTF-8");
    }
}
