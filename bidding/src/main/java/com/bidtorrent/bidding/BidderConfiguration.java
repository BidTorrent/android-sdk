package com.bidtorrent.bidding;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BidderConfiguration {

    private static Random random = ThreadLocalRandom.current();

    private String endPoint;
    private BidderConfigurationFilters filters;
    private String key;

    public BidderConfiguration(String endPoint, BidderConfigurationFilters filters, String key) {
        this.endPoint = endPoint;
        this.filters = filters;
        this.key = key;
    }

    public boolean canBeUsed(){
        return random.nextFloat() <= this.filters.getSampling();
    }

    public String getEndPoint() {
        return endPoint;
    }

    public BidderConfigurationFilters getFilters() {
        return filters;
    }

    public String getKey() {
        return key;
    }
}
