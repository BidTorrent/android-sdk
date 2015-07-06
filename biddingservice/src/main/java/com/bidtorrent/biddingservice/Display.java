package com.bidtorrent.biddingservice;

public class Display {
    private int requesterId;
    private String fileName;

    public Display(int requesterId, String fileName) {
        this.requesterId = requesterId;
        this.fileName = fileName;
    }

    public int getRequesterId() {
        return requesterId;
    }

    public String getFileName() {
        return fileName;
    }
}
