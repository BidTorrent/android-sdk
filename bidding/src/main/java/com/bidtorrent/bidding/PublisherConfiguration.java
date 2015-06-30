package com.bidtorrent.bidding;

public class PublisherConfiguration {

    private String[] advertisersBlacklist;
    private String[] biddersBlacklist;
    private String[] categoriesBlacklist;
    private String[] mediaBlacklist;
    private String country;
    private String currency;
    private float floor;
    private int softTimeout;
    private int hardTimeout;
    private int maximumBidders;

    public PublisherConfiguration(
            String[] advertisersBlacklist,
            String[] biddersBlacklist,
            String[] categoriesBlacklist,
            String[] mediaBlacklist,
            String country,
            String currency,
            float floor,
            int softTimeout,
            int hardTimeout,
            int maximumBidders) {
        this.advertisersBlacklist = advertisersBlacklist;
        this.biddersBlacklist = biddersBlacklist;
        this.categoriesBlacklist = categoriesBlacklist;
        this.mediaBlacklist = mediaBlacklist;
        this.country = country;
        this.currency = currency;
        this.floor = floor;
        this.softTimeout = softTimeout;
        this.hardTimeout = hardTimeout;
        this.maximumBidders = maximumBidders;
    }

    public int getMaximumBidders() {
        return maximumBidders;
    }

    public String[] getAdvertisersBlacklist() {
        return advertisersBlacklist;
    }

    public String[] getBiddersBlacklist() {
        return biddersBlacklist;
    }

    public String[] getCategoriesBlacklist() {
        return categoriesBlacklist;
    }

    public String[] getMediaBlacklist() {
        return mediaBlacklist;
    }

    public String getCountry() {
        return country;
    }

    public String getCurrency() {
        return currency;
    }

    public float getFloor() {
        return floor;
    }

    public int getSoftTimeout() {
        return softTimeout;
    }

    public int getHardTimeout() {
        return hardTimeout;
    }
}
