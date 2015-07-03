package com.bidtorrent.bidding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public class BidderSelector {

    private Collection<com.bidtorrent.bidding.messages.configuration.BidderConfiguration> bidders;
    private com.bidtorrent.bidding.messages.configuration.PublisherConfiguration publisherConfiguration;

    public BidderSelector(com.bidtorrent.bidding.messages.configuration.PublisherConfiguration publisherConfiguration){
        this.publisherConfiguration = publisherConfiguration;
        this.bidders = new LinkedBlockingQueue<>();
    }

    public void addBidder(com.bidtorrent.bidding.messages.configuration.BidderConfiguration bidder){
        if (bidder.getFilters() != null) {
            if (bidder.getFilters().getCountriesBlacklist().contains(publisherConfiguration.site.publisher.country))
                return; //Country blacklisted

            if (!bidder.getFilters().getCountriesWhitelist().isEmpty()
                    && !bidder.getFilters().getCountriesWhitelist().contains(publisherConfiguration.site.publisher.country))
                return; //Country not in whitelist

            String lang = Locale.getDefault().getDisplayLanguage();

            if (bidder.getFilters().getLanguagesBlacklist().contains(lang))
                return; //Country blacklisted

            if (!bidder.getFilters().getLanguagesBlacklist().isEmpty()
                    && !bidder.getFilters().getLanguagesBlacklist().contains(lang))
                return; //Country not in whitelist
        }

        this.bidders.add(bidder);
    }

    public List<com.bidtorrent.bidding.messages.configuration.BidderConfiguration> getAvailableBidders(){
        LinkedList<com.bidtorrent.bidding.messages.configuration.BidderConfiguration> bidderConfigurations = new LinkedList<>(this.bidders);
        Collections.shuffle(bidderConfigurations);

        List<com.bidtorrent.bidding.messages.configuration.BidderConfiguration> availableBidders = new ArrayList<>(publisherConfiguration.maximumBidders);

        for(com.bidtorrent.bidding.messages.configuration.BidderConfiguration bidder : bidderConfigurations){
            if (!bidder.canBeUsed()){
                continue;
            }

            availableBidders.add(bidder);
            if (availableBidders.size() > publisherConfiguration.maximumBidders)
                return availableBidders;
        }

        return availableBidders;
    }
}
