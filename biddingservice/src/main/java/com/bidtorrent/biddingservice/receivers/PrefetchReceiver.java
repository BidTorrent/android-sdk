package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.prefetching.JavaScriptReadyListener;

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
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
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
}
