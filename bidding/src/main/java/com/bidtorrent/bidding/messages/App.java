package com.bidtorrent.bidding.messages;

public class App {

    public final String domain;
    public final int mobile;
    public final Publisher publisher;

    public App(String domain, int mobile, Publisher publisher) {
        this.domain = domain;
        this.mobile = mobile;
        this.publisher = publisher;
    }
}
