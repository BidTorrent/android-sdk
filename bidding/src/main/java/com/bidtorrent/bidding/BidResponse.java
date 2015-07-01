package com.bidtorrent.bidding;

public class BidResponse implements Comparable  {

    private long bidderId;
    private final Float price;
    private final Integer bidId;
    private final String domain;
    private final String creative;
    private final String notify;

    public BidResponse(long bidderId, float bidPrice, int bidId, String domain, String creative, String notificationUrl) {
        this.bidderId = bidderId;
        this.price = bidPrice;
        this.bidId = bidId;
        this.domain = domain;
        this.creative = creative;
        this.notify = notificationUrl;
    }

    public boolean isValid()
    {
        return price != null && creative != null && notify != null;
    }

    public float getPrice() {
        return price;
    }

    public long getBidderId() {
        return bidderId;
    }
    public void setBidderId(long bidderId)
    {
        this.bidderId = bidderId;
    }

    @Override
    public int compareTo(Object o) {
        BidResponse s = (BidResponse) o;

        if (s.price < this.price)
            return -1;

        if (s.price > this.price)
            return 1;

        return 0;
    }

    public String buildNotificationUrl(String requestId, String impId, long runnerUpId){
        if (this.notify == null || this.notify.equals(""))
            return null;

        return this.notify
                .replaceAll("\\$\\{AUCTION_ID\\}", requestId)
                .replaceAll("\\$\\{AUCTION_BID_ID\\}", "" + this.bidId)
                .replaceAll("\\$\\{AUCTION_IMP_ID\\}", impId)
                .replaceAll("\\$\\{AUCTION_PRICE\\}", String.format("%.2f", this.price))
                .replaceAll("\\$\\{AUCTION_RUNNER_UP\\}", "" + runnerUpId);
    }
}
