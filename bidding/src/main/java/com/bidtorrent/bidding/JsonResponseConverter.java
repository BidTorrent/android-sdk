package com.bidtorrent.bidding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonResponseConverter implements IResponseConverter<String> {
    @Override
    public BidResponse convert(String rawResponse, long bidderId) {
        Gson gson;
        BidResponse response;

        gson = new GsonBuilder().create();
        response = gson.fromJson(rawResponse, BidResponse.class);
        response.setBidderId(bidderId);

        return response;
    }
}
