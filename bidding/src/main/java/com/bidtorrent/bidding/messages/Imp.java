package com.bidtorrent.bidding.messages;

public class Imp {

    public Banner banner;
    public float bidfloor;
    public String id;
    public int instl;
    public boolean secure;

    public Imp(Banner banner, float bidfloor, String id, int instl, boolean secure) {
        this.banner = banner;
        this.bidfloor = bidfloor;
        this.id = id;
        this.instl = instl;
        this.secure = secure;
    }
}
