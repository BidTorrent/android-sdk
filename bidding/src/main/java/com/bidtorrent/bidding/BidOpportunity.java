package com.bidtorrent.bidding;


public class BidOpportunity {

    private int width;
    private int height;
    private String appName;

    public BidOpportunity(Size size, String appName) {
        this.width = size.getWidth();
        this.height = size.getHeight();
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        BidOpportunity that = (BidOpportunity) o;

        if (width != that.width) return false;
        if (height != that.height) return false;
        return !(appName != null ? !appName.equals(that.appName) : that.appName != null);

    }

    @Override
    public int hashCode() {
        int result = width;
        result = 31 * result + height;
        result = 31 * result + (appName != null ? appName.hashCode() : 0);
        return result;
    }
}
