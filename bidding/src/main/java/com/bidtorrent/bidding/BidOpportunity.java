package com.bidtorrent.bidding;


public class BidOpportunity {

    private int width;
    private int height;
    private String appName;

    public BidOpportunity(Size size, String appName) {
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
