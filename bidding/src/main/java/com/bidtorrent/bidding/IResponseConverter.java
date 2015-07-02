package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;

public interface IResponseConverter<T> {
    BidResponse convert(T response, long bidderId);
}
