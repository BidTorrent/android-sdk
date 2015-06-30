package com.bidtorrent.bidding;

import java.util.Collection;

/**
 * Created by x.noelle on 30/06/2015.
 */
public class BidderConfigurationFilters {
    private float sampling;
    private Collection<String> countriesBlacklist;
    private Collection<String> countriesWhitelist;
    private Collection<String> languagesBlacklist;
    private Collection<String> languagesWhitelist;
    private Collection<String> categoriesBlacklist;
    private Collection<String> categoriesWhitelist;
    private Collection<String> publishersBlacklist;

    public BidderConfigurationFilters(float sampling, Collection<String> countriesBlacklist, Collection<String> countriesWhitelist, Collection<String> languagesBlacklist, Collection<String> languagesWhitelist, Collection<String> categoriesBlacklist, Collection<String> categoriesWhitelist, Collection<String> publishersBlacklist) {
        this.sampling = sampling;
        this.countriesBlacklist = countriesBlacklist;
        this.countriesWhitelist = countriesWhitelist;
        this.languagesBlacklist = languagesBlacklist;
        this.languagesWhitelist = languagesWhitelist;
        this.categoriesBlacklist = categoriesBlacklist;
        this.categoriesWhitelist = categoriesWhitelist;
        this.publishersBlacklist = publishersBlacklist;
    }

    public float getSampling() {
        return sampling;
    }

    public Collection<String> getCountriesBlacklist() {
        return countriesBlacklist;
    }

    public Collection<String> getCountriesWhitelist() {
        return countriesWhitelist;
    }

    public Collection<String> getLanguagesBlacklist() {
        return languagesBlacklist;
    }

    public Collection<String> getLanguagesWhitelist() {
        return languagesWhitelist;
    }

    public Collection<String> getCategoriesBlacklist() {
        return categoriesBlacklist;
    }

    public Collection<String> getCategoriesWhitelist() {
        return categoriesWhitelist;
    }

    public Collection<String> getPublishersBlacklist() {
        return publishersBlacklist;
    }
}
