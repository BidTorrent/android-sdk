package com.bidtorrent.bidding.messages.configuration;

import com.bidtorrent.bidding.messages.App;
import com.bidtorrent.bidding.messages.Ext;
import com.bidtorrent.bidding.messages.Imp;

import java.util.ArrayList;
import java.util.List;

public class PublisherConfiguration {
    public App app;
    public List<String> badv;
    public List<String> bcat;
    public String cur;
    public List<Imp> imp;
    public int tmax;
    public Ext ext;
    public String passback;

    public PublisherConfiguration(){
        this.app = new App();
        this.badv = new ArrayList<>();
        this.bcat = new ArrayList<>();
        this.cur = "";
        this.imp = new ArrayList<>();
        this.tmax = -1;
        this.ext = new Ext(-1);
        this.passback = "";
    }

    public static int maximumBidders = 5;
}
