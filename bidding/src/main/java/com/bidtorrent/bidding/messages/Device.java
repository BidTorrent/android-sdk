package com.bidtorrent.bidding.messages;

public class Device {

    public final Geo geo;
    public final String ip;
    public final int js;
    public final String language;
    public final String make;
    public final String model;
    public final String os;
    public final String ua;

    public Device(Geo geo, String ip, int js, String language, String make, String model, String os, String ua) {
        this.geo = geo;
        this.ip = ip;
        this.js = js;
        this.language = language;
        this.make = make;
        this.model = model;
        this.os = os;
        this.ua = ua;
    }

    static Device GetCurrentDevice()
    {
        return new Device(
            new Geo("USA"),
            "91.199.242.236",
            1,
            "en",
            "Apple",
            "iPhone",
            "iOS",
            "Mozilla/5.0 (iPhone; CPU iPhone OS 6_1_3 like Mac OS X) AppleWebKit/536.26 (KHTML, like Gecko) Mobile/10B329");
    }
}
