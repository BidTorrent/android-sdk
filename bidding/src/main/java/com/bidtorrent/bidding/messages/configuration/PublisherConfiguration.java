package com.bidtorrent.bidding.messages.configuration;

import com.bidtorrent.bidding.messages.Imp;

import java.util.ArrayList;
import java.util.List;

public class PublisherConfiguration {
    public Site site;
    public List<String> badv;
    public List<String> bcat;
    public String cur;
    public List<Imp> imp;
    public int tmax;

    public PublisherConfiguration(){
        this.site = new Site();
        this.badv = new ArrayList<>();
        this.bcat = new ArrayList<>();
        this.imp = new ArrayList<>();
    }
    public static int maximumBidders = 5;
}
