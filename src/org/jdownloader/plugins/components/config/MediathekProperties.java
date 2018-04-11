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

    @Key("itemChannel")
    String getChannel();

    @Key("itemChannel")
    void setChannel(String channel);

    @Key("itemShow")
    String getShow();

    @Key("itemShow")
    void setShow(String show);

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

    @Key("fileExtension")
    String getFileExtension();

    @Key("fileExtension")
    void setFileExtension(String fileExtension);

    @Key("HEIGHT")
    int getHeight();

    @Key("HEIGHT")
    void setHeight(int height);

    @Key("SEASONNUMBER")
    int getSeasonNumber();

    @Key("SEASONNUMBER")
    void setSeasonNumber(int seasonNumber);

    @Key("EPISODENUMBER")
    int getEpisodeNumber();

    @Key("EPISODENUMBER")
    void setEpisodeNumber(int episodeNumber);

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
    void setBitrateVideo(long bitrateVideo);

    @Key("BANDWIDTH")
    long getBandwidth();

    @Key("BANDWIDTH")
    void setBandwidth(long bandwidth);
}
