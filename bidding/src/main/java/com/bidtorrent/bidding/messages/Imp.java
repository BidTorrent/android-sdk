package com.bidtorrent.bidding.messages;

public class Imp {

    public final Banner banner;
    public final float bidfloor;
    public final String id;
    public final int instl;
    public final boolean secure;

    public Imp(Banner banner, float bidfloor, String id, int instl, boolean secure) {
        this.banner = banner;
        this.bidfloor = bidfloor;
        this.id = id;
        this.instl = instl;
        this.secure = secure;
    }
}
