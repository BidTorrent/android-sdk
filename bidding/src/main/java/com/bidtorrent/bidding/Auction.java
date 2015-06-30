package com.bidtorrent.bidding;

import java.util.List;

public class Auction {
    private final BidOpportunity opportunity;
    private final List<IBidder> bidders;
    private final float floor;

    public Auction(BidOpportunity opportunity, List<IBidder> bidders, float floor)
    {
        this.opportunity = opportunity;
        this.bidders = bidders;
        this.floor = floor;
    }

    public List<IBidder> getBidders() {
        return bidders;
    }

    public BidOpportunity getOpportunity() {
        return opportunity;
    }

    public float getFloor() {
        return floor;
    }
}
