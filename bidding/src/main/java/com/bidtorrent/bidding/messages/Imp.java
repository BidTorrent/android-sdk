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

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Imp that = (Imp)o;
        return this.id.equals(that.id);
    }

    @Override
    public int hashCode()
    {
        return this.id.hashCode();
    }
}
