package com.bidtorrent.bidding;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class JsonResponseConverter implements IResponseConverter<String> {
    @Override
    public BidResponse convert(String rawResponse, long bidderId) {
        JsonObject parsedResponse;
        Gson gson;

        gson = new GsonBuilder().create();
        parsedResponse = gson.fromJson(rawResponse, JsonObject.class);

        return buildResponseFromJson(parsedResponse, bidderId);
    }

    private static BidResponse buildResponseFromJson(JsonObject parsedResponse, long bidderId) {
        float bidPrice = parsedResponse.get("price").getAsFloat();
        String creative = parsedResponse.get("creative").getAsString();
        String notificationUrl = parsedResponse.get("notify").getAsString();

        return new BidResponse(bidderId, bidPrice, creative, notificationUrl);
    }
}
