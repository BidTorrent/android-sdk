package com.bidtorrent.bidding.messages;

import java.util.ArrayList;

public class BidResponse implements Comparable  {

    public long bidderId;
    public String id;
    public String cur;
    public ArrayList<Seatbid> seatbid;

    public BidResponse(long bidderId, float bidPrice, int bidId, String domain, String creative, String notificationUrl) {
        this.bidderId = bidderId;
        Bid bid = new Bid("" + bidId, "", bidPrice, "", notificationUrl, domain, "");
        this.seatbid = new ArrayList<>(1);
        this.seatbid.add(new Seatbid(bid));
    }



    public boolean isValid()
    {
        Bid bid = this.seatbid.get(0).bid.get(0);
        return  bid != null;
    }

    public float getPrice() {
        return this.seatbid.get(0).bid.get(0).price;
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

        if (s.getPrice() < this.getPrice())
            return -1;

        if (s.getPrice() > this.getPrice())
            return 1;

        return 0;
    }

    public String buildNotificationUrl(String requestId, String impId, long runnerUpId){
        if (this.seatbid.get(0).bid.get(0).nurl == null || this.seatbid.get(0).bid.get(0).nurl.equals(""))
            return null;

        return this.seatbid.get(0).bid.get(0).nurl
                .replaceAll("\\$\\{AUCTION_ID\\}", requestId)
                .replaceAll("\\$\\{AUCTION_BID_ID\\}", "" + this.seatbid.get(0).bid.get(0).id)
                .replaceAll("\\$\\{AUCTION_IMP_ID\\}", impId)
                .replaceAll("\\$\\{AUCTION_PRICE\\}", String.format("%.2f", this.seatbid.get(0).bid.get(0).price))
                .replaceAll("\\$\\{AUCTION_RUNNER_UP\\}", "" + runnerUpId);
    }
}
