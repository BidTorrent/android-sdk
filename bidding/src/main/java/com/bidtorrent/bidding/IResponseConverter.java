package com.bidtorrent.bidding;

public interface IResponseConverter<T> {
    BidResponse convert(T response, long bidderId);
}
