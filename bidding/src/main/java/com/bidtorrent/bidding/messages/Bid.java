package com.bidtorrent.bidding.messages;

public class Bid {
    public String id;
    public String impid;
    public float price;
    public String signature;
    public String nurl;
    public String adomain;
    public String creative;

    public Bid(String id, String impid, float price, String signature, String nurl, String adomain, String creative) {
        this.id = id;
        this.impid = impid;
        this.price = price;
        this.signature = signature;
        this.nurl = nurl;
        this.adomain = adomain;
        this.creative = creative;
    }
}
