package com.bidtorrent.bidding.messages;

import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;

import java.io.Serializable;

public class ContextualizedBidResponse implements Comparable, Serializable{

    private BidResponse bidResponse;
    private BidderConfiguration bidderConfiguration;

    public ContextualizedBidResponse(BidResponse bidResponse, BidderConfiguration bidderConfiguration) {
        this.bidResponse = bidResponse;
        this.bidderConfiguration = bidderConfiguration;
    }

    public BidderConfiguration getBidderConfiguration() {
        return bidderConfiguration;
    }

    public BidResponse getBidResponse() {
        return bidResponse;
    }

    @Override
    public int compareTo(Object o) {
        return this.bidResponse.compareTo(((ContextualizedBidResponse)o).bidResponse);
    }
}
