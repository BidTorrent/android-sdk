package com.bidtorrent.bidding.messages;

import java.util.ArrayList;

public class Seatbid {
    public ArrayList<Bid> bid;

    public Seatbid(Bid bid) {
        this.bid = new ArrayList<Bid>();
        this.bid.add(bid);
    }
}
