package com.bidtorrent.biddingservice.pooling;

public interface MyTwoParametersFunction<T, T1> {
    void apply(T myFirstParameter, T1 mySecondParameter);
}
