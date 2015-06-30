package com.bidtorrent.bidding;


public class BidOpportunity {

    private int width;
    private int height;
    private String appName;

    public BidOpportunity(int width, int height, String appName) {
        this.width = width;
        this.height = height;
        this.appName = appName;
    }

    public String getAppName() {
        return appName;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
