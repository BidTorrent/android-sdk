package com.bidtorrent.biddingservice.pooling;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public class PoolSizer {
    private ConnectivityManager connectivityManager;
    private int wifiPoolSize;
    private int dataPoolSize;

    public PoolSizer(ConnectivityManager connectivityManager, int wifiPoolSize, int dataPoolSize) {
        this.connectivityManager = connectivityManager;
        this.wifiPoolSize = wifiPoolSize;
        this.dataPoolSize = dataPoolSize;
    }

    public int getPoolSize()
    {
        switch (getCurrentConnectionType())
        {
            case ConnectivityManager.TYPE_WIFI:
            case ConnectivityManager.TYPE_WIMAX:
            case ConnectivityManager.TYPE_ETHERNET:
            case ConnectivityManager.TYPE_VPN:
                return wifiPoolSize;

            case ConnectivityManager.TYPE_MOBILE_DUN:
            case ConnectivityManager.TYPE_MOBILE:
            case ConnectivityManager.TYPE_BLUETOOTH:
            case ConnectivityManager.TYPE_MOBILE_HIPRI:
            case ConnectivityManager.TYPE_MOBILE_MMS:
            case ConnectivityManager.TYPE_MOBILE_SUPL:
                return dataPoolSize;

            default:
                return 0;
        }
    }

    private int getCurrentConnectionType()
    {
        NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();

        return activeNetwork == null ? -1 : activeNetwork.getType();
    }
}
