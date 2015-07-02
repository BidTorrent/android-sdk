package com.bidtorrent.bidding;

import com.bidtorrent.bidding.messages.BidResponse;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class JsonResponseConverter implements IResponseConverter<String> {
    @Override
    public BidResponse convert(String rawResponse, long bidderId) {
        Gson gson;
        BidResponse response = null;

        gson = new GsonBuilder().create();
        try {
            response = gson.fromJson(rawResponse, BidResponse.class);
            response.setBidderId(bidderId);
        } catch (Exception e){
            e.printStackTrace();
        }

        return response;
    }
}
