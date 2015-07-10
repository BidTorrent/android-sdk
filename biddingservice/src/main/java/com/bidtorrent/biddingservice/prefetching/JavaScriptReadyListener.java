package com.bidtorrent.biddingservice.prefetching;

import android.webkit.JavascriptInterface;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import java.util.concurrent.atomic.AtomicBoolean;

public class JavaScriptReadyListener {

    private static final String IMAGE_FUNCTIONS = "javascript:function isImageOk(img) {\n" +
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
            "}\n";

    private static final String READY_MONITOR_FUNCTION = "javascript:var interval = setInterval(function() { \n" +
            "  if (allImagesOK()){\n" +
            "    window.clearInterval(interval);\n" +
            "    jsListener.setDone();\n" +
            "  }" +
            "}, 100);";

    private AtomicBoolean done;
    private Runnable pageSaver;

    private JavaScriptReadyListener(Runnable pageSaver) {
        this.pageSaver = pageSaver;
        this.done = new AtomicBoolean(false);
    }

    @JavascriptInterface
    public void setDone(){
        if (!this.done.getAndSet(true)) {
            this.sendPrefetchedPage();
        }
    }

    private void sendPrefetchedPage() {
        this.pageSaver.run();
    }

    public static void hookReadyEvent(WebView webView, Runnable onDoneEvent){
        final JavaScriptReadyListener jsListener = new JavaScriptReadyListener(onDoneEvent);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        webView.addJavascriptInterface(jsListener, "jsListener");

        webView.setWebViewClient(new WebViewClient() {

            @Override
            public void onPageFinished(WebView view, String url) {
                view.loadUrl(IMAGE_FUNCTIONS);
                view.loadUrl(READY_MONITOR_FUNCTION);

            }

            @Override
            public WebResourceResponse shouldInterceptRequest(WebView view, WebResourceRequest request) {
                return super.shouldInterceptRequest(view, request);
            }
        });
    }
}