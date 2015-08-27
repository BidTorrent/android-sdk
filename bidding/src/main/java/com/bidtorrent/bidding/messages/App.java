package com.bidtorrent.bidding.messages;

import java.util.ArrayList;
import java.util.List;

public class App {
    public List<String> cat;
    public String domain;
    public Publisher publisher;

    public App(){
        this.cat = new ArrayList<>();
        this.domain = "";
        this.publisher = new Publisher("", "");
    }

    public App(String domain, Publisher publisher) {
        this.cat = new ArrayList<>();
        this.domain = domain;
        this.publisher = publisher;
    }
}
