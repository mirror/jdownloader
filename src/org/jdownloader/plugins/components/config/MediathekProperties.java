package org.jdownloader.plugins.components.config;

import jd.plugins.DownloadLinkDatabindingInterface;

public interface MediathekProperties extends DownloadLinkDatabindingInterface {
    @Key("itemTitle")
    String getTitle();

    @Key("itemTitle")
    void setTitle(String title);

    @Key("itemSrc")
    String getSourceHost();

    @Key("itemSrc")
    void setSourceHost(String host);

    @Key("itemSrc")
    String getChannel();

    @Key("itemSrc")
    void setChannel(String channel);

    @Key("itemShow")
    String getShow();

    @Key("itemShow")
    void setShow(String channel);

    @Key("itemRes")
    String getResolution();

    @Key("itemRes")
    void setResolution(String resolution);

    @Key("itemType")
    String getProtocol();

    @Key("itemType")
    void setProtocol(String protocol);

    @Key("itemStreamingType")
    String getStreamingType();

    @Key("itemStreamingType")
    void setStreamingType(String streamingType);

    @Key("HEIGHT")
    int getHeight();

    @Key("HEIGHT")
    void setHeight(int height);

    @Key("WIDTH")
    int getWidth();

    @Key("WIDTH")
    void setWidth(int height);

    @Key("DATE_RELEASED")
    long getReleaseDate();

    @Key("DATE_RELEASED")
    void setReleaseDate(long releaseDate);

    @Key("BITRATE_TOTAL")
    long getBitrateTotal();

    @Key("BITRATE_TOTAL")
    void setBitrateTotal(long bitrateTotal);

    @Key("BITRATE_VIDEO")
    long getBitrateVideo();

    @Key("BITRATE_VIDEO")
    void getBitrateVideo(long bitrateVideo);
}
