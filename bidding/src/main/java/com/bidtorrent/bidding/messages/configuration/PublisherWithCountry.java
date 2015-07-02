package com.bidtorrent.bidding.messages.configuration;

import com.bidtorrent.bidding.messages.Publisher;

public class PublisherWithCountry extends Publisher{

    public String country;

    public PublisherWithCountry(String id, String name) {
        super(id, name);
    }
}
