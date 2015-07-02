package com.bidtorrent.bidding.messages;

public class User {

    public final String id;
    public final String buyerid;

    public User(String id, String buyerid) {
        this.id = id;
        this.buyerid = buyerid;
    }
}
