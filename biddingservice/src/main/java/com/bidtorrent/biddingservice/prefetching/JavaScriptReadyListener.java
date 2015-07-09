package com.bidtorrent.biddingservice.prefetching;

import android.webkit.JavascriptInterface;

import java.util.concurrent.atomic.AtomicBoolean;

public class JavaScriptReadyListener {
    private AtomicBoolean done;
    private Runnable pageSaver;

    public JavaScriptReadyListener(Runnable pageSaver) {
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
}