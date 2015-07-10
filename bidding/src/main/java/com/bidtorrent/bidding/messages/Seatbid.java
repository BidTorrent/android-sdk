package com.bidtorrent.bidding.messages;

import java.io.Serializable;
import java.util.ArrayList;

public class Seatbid implements Serializable {
    public ArrayList<Bid> bid;

    public Seatbid(Bid bid) {
        this.bid = new ArrayList<Bid>();
        this.bid.add(bid);
    }
}
