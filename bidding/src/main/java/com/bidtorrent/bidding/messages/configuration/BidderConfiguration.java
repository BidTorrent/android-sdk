package com.bidtorrent.bidding.messages.configuration;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BidderConfiguration {

    private static Random random = ThreadLocalRandom.current();

    public long id;
    public String bid_ep;
    public String name;
    public BidderConfigurationFilters filters;
    public String key;

    public BidderConfiguration(String bid_ep, BidderConfigurationFilters filters, String key) {
        this.bid_ep = bid_ep;
        this.filters = filters;
        this.key = key;
    }

    public boolean canBeUsed(){
        if (this.filters == null) return true;
        return random.nextFloat() <= this.filters.getSampling();
    }

    public String getBid_ep() {
        return bid_ep;
    }

    public BidderConfigurationFilters getFilters() {
        return filters;
    }

    public String getKey() {
        return key;
    }
}
