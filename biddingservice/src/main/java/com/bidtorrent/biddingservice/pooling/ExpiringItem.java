package com.bidtorrent.biddingservice.pooling;

import java.util.Date;

public class ExpiringItem implements Comparable<ExpiringItem> {
    protected Date expirationDate;

    protected ExpiringItem(Date expirationDate) {
        this.expirationDate = expirationDate;
    }

    public boolean isExpired()
    {
        return expirationDate.before(new Date());
    }

    @Override
    public int compareTo(ExpiringItem another) {
        return this.expirationDate.compareTo(another.expirationDate);
    }
}