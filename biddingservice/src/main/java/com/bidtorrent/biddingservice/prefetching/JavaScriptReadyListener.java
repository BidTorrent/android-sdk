package com.bidtorrent.biddingservice.prefetching;

import android.webkit.JavascriptInterface;

public class JavaScriptReadyListener {
    private Runnable pageSaver;

    public JavaScriptReadyListener(Runnable pageSaver) {
        this.pageSaver = pageSaver;
    }

    @JavascriptInterface
    public void notifyLoadingError()
    {
        // FIXME: do something meaningful
        System.out.println("ERROR");
    }

    @JavascriptInterface
    public void notifyLoaded(){
        this.sendPrefetchedPage();
    }

    private void sendPrefetchedPage() {
        this.pageSaver.run();
    }
}