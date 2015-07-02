package com.bidtorrent.bidding.messages.configuration;

import com.bidtorrent.bidding.messages.Imp;

import java.util.List;

public class PublisherConfiguration {

    public Site site;
    public List<String> badv;
    public List<String> bcat;
    public String cur;
    public Imp imp;
    public int timeout_soft;
    public int timeout_hard;
    public int maximumBidders = 5;




}
