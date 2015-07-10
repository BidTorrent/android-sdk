package com.bidtorrent.biddingservice.prefetching;

import android.webkit.JavascriptInterface;

public class JavaScriptReadyListener {
    private Runnable pageSaver;
    private Runnable failureHandler;

    public JavaScriptReadyListener(Runnable pageSaver, Runnable failureHandler) {
        this.pageSaver = pageSaver;
        this.failureHandler = failureHandler;
    }

    @JavascriptInterface
    public void notifyLoadingError(){
        this.failureHandler.run();
    }

    @JavascriptInterface
    public void notifyLoaded(){
        this.sendPrefetchedPage();
    }

    private void sendPrefetchedPage() {
        this.pageSaver.run();
    }
}