package com.bidtorrent.bidding.messages.configuration;

import java.util.Collection;
import java.util.Collections;

/**
 * Created by x.noelle on 30/06/2015.
 */
public class BidderConfigurationFilters {
    private float sampling;
    private Collection<String> ctry_bl = Collections.emptyList();
    private Collection<String> ctry_wl = Collections.emptyList();
    private Collection<String> lang_bl = Collections.emptyList();
    private Collection<String> lang_wl = Collections.emptyList();
    private Collection<String> cat_bl = Collections.emptyList();
    private Collection<String> cat_wl = Collections.emptyList();
    private Collection<String> pub = Collections.emptyList();

    public BidderConfigurationFilters(float sampling, Collection<String> countriesBlacklist, Collection<String> countriesWhitelist, Collection<String> languagesBlacklist, Collection<String> languagesWhitelist, Collection<String> categoriesBlacklist, Collection<String> categoriesWhitelist, Collection<String> publishersBlacklist) {
        this.sampling = sampling;
        this.ctry_bl = countriesBlacklist;
        this.ctry_wl = countriesWhitelist;
        this.lang_bl = languagesBlacklist;
        this.lang_wl = languagesWhitelist;
        this.cat_bl = categoriesBlacklist;
        this.cat_wl = categoriesWhitelist;
        this.pub = publishersBlacklist;
    }

    public float getSampling() {
        return sampling;
    }

    public Collection<String> getCountriesBlacklist() {
        return ctry_bl;
    }

    public Collection<String> getCountriesWhitelist() {
        return ctry_wl;
    }

    public Collection<String> getLanguagesBlacklist() {
        return lang_bl;
    }

    public Collection<String> getLanguagesWhitelist() {
        return lang_wl;
    }

    public Collection<String> getCategoriesBlacklist() {
        return cat_bl;
    }

    public Collection<String> getCategoriesWhitelist() {
        return cat_wl;
    }

    public Collection<String> getPublishersBlacklist() {
        return pub;
    }
}