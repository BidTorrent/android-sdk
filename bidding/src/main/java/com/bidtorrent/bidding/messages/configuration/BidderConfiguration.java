package com.bidtorrent.bidding.messages.configuration;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

public class BidderConfiguration {
    public Long id;
    public String bid_ep;
    public String name;
    public BidderConfigurationFilters filters;
    public String key;
}
