package com.bidtorrent.bidding.messages.configuration;

import java.io.Serializable;

public class BidderConfiguration implements Serializable {
    public Long id;
    public String bid_ep;
    public String name;
    public BidderConfigurationFilters filters;
    public String key;
}
