package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.Imp;

import java.util.List;

public class Auction {
    private final List<IBidder> bidders;
    private final Imp impression;

    public Auction(Imp impressionId, List<IBidder> bidders)
    {
        this.bidders = bidders;
        this.impression = impressionId;
    }

    public Imp getImpression() {
        return this.impression;
    }

    public List<IBidder> getBidders() {
        return this.bidders;
    }

}
