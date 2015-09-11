package com.bidtorrent.biddingservice;

public final class Constants {
    // Activity extras
    public static final String AUCTION_ERROR_REASON_ARG = "reason";
    public static final String CREATIVE_CODE_ARG = "creativeCode";
    public static final String PREFETCHED_CREATIVE_FILE_ARG = "creativeFile";
    public static final String PREFETCHED_CREATIVE_EXPIRATION_ARG = "dontexpireme";
    public static final String REQUESTER_ID_ARG = "requesterId";
    public static final String NOTIFICATION_URL_ARG = "notif";
    public static final String AUCTION_ID_ARG = "auction-id";
    public static final String AUCTION_RESULT_ARG = "auctionResult";
    public static final String PASSBACK_URL_ARG = "fallback-url";
    public static final String IMPRESSION_ID_ARG = "impressionId";

    // Intents
    public static final String BID_AVAILABLE_INTENT = "bid-available";
    public static final String AUCTION_FAILED_INTENT = "auction-failed";
    public static final String DISPLAY_PASSBACK_AD_INTENT = "fallback-please";
    public static final String READY_TO_DISPLAY_AD_INTENT = "ready-to-display";

    // Actions
    public static final String BID_ACTION = "please-bid";
    public static final String FILL_PREFETCH_BUFFER_ACTION = "please-store";
    public static final String PREFETCH_FAILED_ACTION = "prefetch-failed";
    public static final String NOTIFICATION_ACTION = "notify";
}
