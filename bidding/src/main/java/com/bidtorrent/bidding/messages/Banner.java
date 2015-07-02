package com.bidtorrent.bidding.messages;

import java.util.List;

/**
 * Created by m.waisgold on 01/07/2015.
 */
public class Banner {

    public final List<Integer> btype;
    public final int h;
    public final int pos;
    public final int w;

    public Banner(List<Integer> btype, int h, int pos, int w) {
        this.btype = btype;
        this.h = h;
        this.pos = pos;
        this.w = w;
    }
}
