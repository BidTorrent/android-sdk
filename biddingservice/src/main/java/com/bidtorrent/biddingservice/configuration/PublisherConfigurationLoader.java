package com.bidtorrent.biddingservice.configuration;

import android.content.res.AssetManager;
import android.net.Uri;
import android.util.Log;

import com.bidtorrent.bidding.PooledHttpClient;
import com.bidtorrent.bidding.messages.App;
import com.bidtorrent.bidding.messages.Banner;
import com.bidtorrent.bidding.messages.Ext;
import com.bidtorrent.bidding.messages.Imp;
import com.bidtorrent.bidding.messages.Publisher;
import com.bidtorrent.bidding.messages.configuration.BidderConfiguration;
import com.bidtorrent.bidding.messages.configuration.PublisherConfiguration;
import com.google.common.base.Function;
import com.google.common.util.concurrent.AsyncFunction;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

public class PublisherConfigurationLoader {

    private String assetsPath;
    private AssetManager assetManager;
    private PooledHttpClient httpClient;
    private String publisherConfigurationServer;

    public PublisherConfigurationLoader(String assetsPath, AssetManager assetManager, PooledHttpClient httpClient, String publisherConfigurationServer)
    {
        this.assetsPath = assetsPath;
        this.assetManager = assetManager;
        this.httpClient = httpClient;
        this.publisherConfigurationServer = publisherConfigurationServer;
    }

    public ListenableFuture<PublisherConfiguration> loadConfiguration()
    {
        ListenableFuture<PublisherConfiguration> distantConfiguration;
        final PublisherConfiguration localConfiguration;

        localConfiguration = this.loadLocalConfiguration();
        distantConfiguration = this.downloadDistantConfiguration(localConfiguration.app.publisher.id);

        Futures.addCallback(distantConfiguration, new FutureCallback<PublisherConfiguration>() {
            @Override
            public void onSuccess(PublisherConfiguration result) {}

            @Override
            public void onFailure(Throwable t) {
                Log.e("PublisherConfigurationLoader", "Failed to download publisher configuration", t);
            }
        });

        return Futures.transform(distantConfiguration,
                new Function<PublisherConfiguration, PublisherConfiguration>() {
                    @Override
                    public PublisherConfiguration apply(PublisherConfiguration distantConfiguration){
                        return PublisherConfigurationLoader.mergeConfigurations(distantConfiguration, localConfiguration);
                    }
                });
    }

    private static List<Integer> readBType(Properties properties) {
        List<Integer> list = new ArrayList<>();

        for (String s : properties.getProperty("imp.banner.btype").split(",")){
            list.add(Integer.parseInt(s));
        }

        return list;
    }

    private ListenableFuture<PublisherConfiguration> downloadDistantConfiguration(String publisherId)
    {
        return this.httpClient.jsonGet(this.publisherConfigurationServer.toString() + publisherId, PublisherConfiguration.class);
    }

    private PublisherConfiguration loadConfigurationFromProperties(Properties properties)
    {
        PublisherConfiguration configuration = new PublisherConfiguration();

        configuration.app = new App();
        configuration.app.cat = Arrays.asList(properties.getProperty("app.cat").split(","));
        configuration.app.domain = properties.getProperty("app.domain");

        configuration.app.publisher = new Publisher(
                properties.getProperty("app.publisher.id"),
                properties.getProperty("app.publisher.name"));

        configuration.badv = Arrays.asList(properties.getProperty("badv").split(","));
        configuration.bcat = Arrays.asList(properties.getProperty("bcat").split(","));
        configuration.cur = properties.getProperty("cur");

        configuration.imp = Arrays.asList(
                new Imp(
                        new Banner(
                                PublisherConfigurationLoader.readBType(properties),
                                Integer.parseInt(properties.getProperty("imp.banner.h", "-1")),
                                Integer.parseInt(properties.getProperty("imp.banner.pos", "-1")),
                                Integer.parseInt(properties.getProperty("imp.banner.w", "-1"))),
                        Float.parseFloat(properties.getProperty("imp.bidfloor", "-1")),
                        "",
                        Integer.parseInt(properties.getProperty("imp.instl", "-1")),
                        Boolean.parseBoolean(properties.getProperty("imp.secure", "false"))));

        configuration.tmax = Integer.parseInt(properties.getProperty("tmax", "-1"));
        configuration.ext = new Ext(Integer.parseInt(properties.getProperty("ext.btid", "-1")));

        if (configuration.app.publisher.id.equals(""))
            throw new IllegalArgumentException("Publisher Id is mandatory for configuration");

        return configuration;
    }

    private PublisherConfiguration loadLocalConfiguration()
    {
        Properties properties;

        try {
            properties = this.loadProperties();
        }
        catch (IOException e) {
            e.printStackTrace();

            //TODO handle invalid configuration
            properties = null;
        }

        return this.loadConfigurationFromProperties(properties);
    }

    private Properties loadProperties() throws IOException {
        Properties properties;
        InputStream stream;

        stream = this.assetManager.open(this.assetsPath);
        properties = new Properties();
        properties.load(stream);

        return properties;
    }

    private static PublisherConfiguration mergeConfigurations(PublisherConfiguration distanceConfiguration,
                                                              PublisherConfiguration localConfiguration) {

        PublisherConfiguration finalConfiguration = distanceConfiguration;

        finalConfiguration.app.cat.addAll(localConfiguration.app.cat);

        if (!localConfiguration.app.domain.equals("")) finalConfiguration.app.domain = localConfiguration.app.domain;
        if (!localConfiguration.app.publisher.id.equals("")) finalConfiguration.app.publisher.id = localConfiguration.app.publisher.id;
        if (!localConfiguration.app.publisher.name.equals("")) finalConfiguration.app.publisher.name = localConfiguration.app.publisher.name;

        finalConfiguration.badv.addAll(localConfiguration.badv);
        finalConfiguration.bcat.addAll(localConfiguration.bcat);

        if (!localConfiguration.cur.equals("")) finalConfiguration.cur = localConfiguration.cur;

        if (finalConfiguration.imp.isEmpty()){
            finalConfiguration.imp.addAll(localConfiguration.imp);
        } else {
            if (finalConfiguration.imp.get(0).banner.btype == null)
                finalConfiguration.imp.get(0).banner.btype = localConfiguration.imp.get(0).banner.btype;
            else
                finalConfiguration.imp.get(0).banner.btype.addAll(localConfiguration.imp.get(0).banner.btype);

            if (localConfiguration.imp.get(0).banner.h != -1) finalConfiguration.imp.get(0).banner.h = localConfiguration.imp.get(0).banner.h;
            if (localConfiguration.imp.get(0).banner.w != -1) finalConfiguration.imp.get(0).banner.w = localConfiguration.imp.get(0).banner.w;
            if (localConfiguration.imp.get(0).banner.pos != -1) finalConfiguration.imp.get(0).banner.pos = localConfiguration.imp.get(0).banner.pos;
            if (localConfiguration.imp.get(0).instl != -1) finalConfiguration.imp.get(0).instl = localConfiguration.imp.get(0).instl;
            if (localConfiguration.imp.get(0).secure) finalConfiguration.imp.get(0).secure = true;
        }

        if (localConfiguration.tmax != -1) finalConfiguration.tmax = localConfiguration.tmax;

        return finalConfiguration;
    }
}