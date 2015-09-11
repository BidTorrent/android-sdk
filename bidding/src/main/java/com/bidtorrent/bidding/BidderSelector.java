package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.Geo;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadLocalRandom;

public class BidderSelector
{
    private Collection<BidderConfiguration> bidders;
    private PublisherConfiguration publisherConfiguration;
    private static Random random = ThreadLocalRandom.current();

    public BidderSelector(PublisherConfiguration publisherConfiguration){
        this.publisherConfiguration = publisherConfiguration;
        this.bidders = new LinkedBlockingQueue<>();
    }

    public boolean acceptBidder(PublisherConfiguration publisherConfiguration, BidderConfiguration bidderConfiguration)
    {
        Collection<String> cat_bl;
        String country;
        String user_lang;

        if(bidderConfiguration.id == null)
            return false;

        if(bidderConfiguration.bid_ep == null || bidderConfiguration.bid_ep.length() == 0)
            return false;

        if(bidderConfiguration.filters == null)
            return true;

        country = Geo.getCurrentCountry();

        if (bidderConfiguration.filters.pub_ctry != null &&
            bidderConfiguration.filters.pub_ctry.size() != 0 &&
            country != null)
        {
            // if whitelist mode
            if (bidderConfiguration.filters.pub_ctry_wl == Boolean.TRUE)
            {
                if (!bidderConfiguration.filters.pub_ctry.contains(country))
                   return false;
            }
            else
            {
                if (bidderConfiguration.filters.pub_ctry.contains(country))
                    return false;
            }
        }

        user_lang = Locale.getDefault().getDisplayLanguage();

        if (bidderConfiguration.filters.user_lang != null &&
            bidderConfiguration.filters.user_lang.size() != 0)
        {
            // if whitelist mode
            if (bidderConfiguration.filters.user_lang_wl != null &&
                bidderConfiguration.filters.user_lang_wl == true)
            {
                if (!bidderConfiguration.filters.user_lang.contains(user_lang))
                    return false;
            }
            else
            {
                if (bidderConfiguration.filters.user_lang.contains(user_lang))
                    return false;
            }
        }

        cat_bl = publisherConfiguration.app.cat;

        if (bidderConfiguration.filters.cat_bl != null &&
            cat_bl != null && cat_bl.size() > 0) {
            for (String cat : cat_bl) {
                if (bidderConfiguration.filters.cat_bl.contains(cat)) {
                    return false;
                }
            }
        }

        return true;
    }

    public void addBidder(BidderConfiguration bidder)
    {
        this.bidders.add(bidder);
    }

    public List<BidderConfiguration> getAvailableBidders()
    {
        LinkedList<BidderConfiguration> bidderConfigurations = new LinkedList<>(this.bidders);
        Collections.shuffle(bidderConfigurations);

        List<BidderConfiguration> availableBidders = new ArrayList<>(publisherConfiguration.maximumBidders);

        for(BidderConfiguration bidder : bidderConfigurations){
            if (bidder.filters != null &&
                bidder.filters.sampling < BidderSelector.random.nextFloat() * 100.f)
                continue;

            availableBidders.add(bidder);

            if (availableBidders.size() > publisherConfiguration.maximumBidders)
                return availableBidders;
        }

        return availableBidders;
    }
}
