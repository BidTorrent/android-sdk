package com.bidtorrent.bidding;

public class BidResponse implements Comparable  {
    private final long bidderId;
    private final float bidPrice;
    private final String creative;
    private final String notificationUrl;

    public BidResponse(long bidderId, float bidPrice, String creative, String notificationUrl) {
        this.bidderId = bidderId;
        this.bidPrice = bidPrice;
        this.creative = creative;
        this.notificationUrl = notificationUrl;
    }

    public float getBidPrice() {
        return bidPrice;
    }

    public long getBidderId() {
        return bidderId;
    }

    @Override
    public int compareTo(Object o) {
        BidResponse s = (BidResponse) o;

        if (s.bidPrice < this.bidPrice)
            return -1;

        if (s.bidPrice > this.bidPrice)
            return 1;

        return 0;
    }
}
