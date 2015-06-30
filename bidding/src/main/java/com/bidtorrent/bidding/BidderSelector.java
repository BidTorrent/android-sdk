package com.bidtorrent.bidding;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.LinkedBlockingQueue;

public class BidderSelector {

    private Collection<BidderConfiguration> bidders;
    private PublisherConfiguration publisherConfiguration;

    public BidderSelector(PublisherConfiguration publisherConfiguration){
        this.publisherConfiguration = publisherConfiguration;
        this.bidders = new LinkedBlockingQueue<>();
    }

    public void addBidder(BidderConfiguration bidder){

        if (bidder.getFilters().getCountriesBlacklist().contains(publisherConfiguration.getCountry()))
            return; //Country blacklisted

        if (!bidder.getFilters().getCountriesWhitelist().isEmpty()
                && !bidder.getFilters().getCountriesWhitelist().contains(publisherConfiguration.getCountry()))
            return; //Country not in whitelist

        String lang = Locale.getDefault().getDisplayLanguage();

        if (bidder.getFilters().getLanguagesBlacklist().contains(lang))
            return; //Country blacklisted

        if (!bidder.getFilters().getLanguagesBlacklist().isEmpty()
                && !bidder.getFilters().getLanguagesBlacklist().contains(lang))
            return; //Country not in whitelist

        this.bidders.add(bidder);
    }

    public List<BidderConfiguration> getAvailableBidders(){
        LinkedList<BidderConfiguration> bidderConfigurations = new LinkedList<>(this.bidders);
        Collections.shuffle(bidderConfigurations);

        List<BidderConfiguration> availableBidders = new ArrayList<>(publisherConfiguration.getMaximumBidders());

        for(BidderConfiguration bidder : bidderConfigurations){
            if (!bidder.canBeUsed()){
                continue;
            }

            availableBidders.add(bidder);
            if (availableBidders.size() > publisherConfiguration.getMaximumBidders())
                return availableBidders;
        }

        return availableBidders;
    }
}
