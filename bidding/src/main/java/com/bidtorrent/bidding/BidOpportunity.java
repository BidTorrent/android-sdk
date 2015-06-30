package com.bidtorrent.bidding;

import java.net.URI;

public class BidOpportunity {
    private URI publisherUri;

    public BidOpportunity(URI publisherUri) {
        this.publisherUri = publisherUri;
    }

    public URI getPublisherUri() {
        return publisherUri;
    }
}
