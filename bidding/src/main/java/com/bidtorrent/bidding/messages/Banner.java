package com.bidtorrent.bidding.messages;

import java.util.List;

public class Banner {

    public List<Integer> btype;
    public int h;
    public int pos;
    public int w;

    public Banner(List<Integer> btype, int h, int pos, int w) {
        this.btype = btype;
        this.h = h;
        this.pos = pos;
        this.w = w;
    }
}
