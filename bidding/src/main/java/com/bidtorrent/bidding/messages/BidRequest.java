package com.bidtorrent.bidding.messages;

import java.util.List;

public class BidRequest {

    public final User user;
    public final App app;
    public final String cur;
    public final Device device;
    public final String id;
    public final List<Imp> imp;
    public final int tmax;
    public final Ext ext;

    public BidRequest(User user, App app, String cur, Device device, String id, List<Imp> imp, int tmax, Ext ext) {
        this.user = user;
        this.app = app;
        this.cur = cur;
        this.device = device;
        this.id = id;
        this.imp = imp;
        this.tmax = tmax;
        this.ext = ext;
    }
}
