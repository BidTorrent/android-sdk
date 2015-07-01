package com.bidtorrent.bidding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

import org.apache.commons.logging.Log;

import java.util.concurrent.Callable;

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
