package com.bidtorrent.biddingservice.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.webkit.ValueCallback;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import com.bidtorrent.biddingservice.BiddingIntentService;
import com.bidtorrent.biddingservice.Constants;
import com.bidtorrent.biddingservice.prefetching.JavaScriptReadyListener;

import java.io.File;
import java.io.IOException;

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
        final JavaScriptReadyListener jsListener = new JavaScriptReadyListener(
                new Runnable()
                {
                    @Override
                    public void run() {
                        sendPrefetchedData(context, webView, intent);
                    }
                });

        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.addJavascriptInterface(jsListener, "jsListener");
        webView.loadData(creative, "text/html", "utf-8");
        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl("javascript:function isImageOk(img) {\n" +
                        "  if (!img.complete)\n" +
                        "    return false;\n" +
                        "\n" +
                        "  if (typeof img.naturalWidth != \"undefined\" && img.naturalWidth == 0)\n" +
                        "    return false;\n" +
                        "\n" +
                        "  return true;\n" +
                        "}\n" +
                        "\n" +
                        "function allImagesOK()\n" +
                        "{\n" +
                        "  var failedImages = 0;\n" +
                        "\n" +
                        "  for (var i = 0; i < document.images.length; i++) {\n" +
                        "    if (!isImageOk(document.images[i]))\n" +
                        "      failedImages += 1;\n" +
                        "  }\n" +
                        "  \n" +
                        "  return (failedImages == 0);\n" +
                        "}\n");
                view.loadUrl("javascript:var interval = setInterval(function() { \n" +
                        "  if (allImagesOK()){\n" +
                        "    window.clearInterval(interval);\n" +
                        "    jsListener.setDone();\n" +
                        "  }" +
                        "}, 100);");

            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return super.shouldInterceptRequest(view, request);
            }
        });
    }

    private void sendPrefetchedData(final Context context, final WebView webView, final Intent intent) {
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
                            }
                        });
                    }
                });
    }
}
