package org.jdownloader.plugins.components.youtube;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public class YoutubeFinalLinkResource implements Storable {
    public final static TypeRef<YoutubeFinalLinkResource> TYPE_REF = new TypeRef<YoutubeFinalLinkResource>() {

                                                                   };
    private YoutubeITAG                                   itag;
    private String                                        videoID;
    private String[]                                      segments;

    public YoutubeITAG getItag() {
        return itag;
    }

    public void setItag(YoutubeITAG itag) {
        this.itag = itag;
    }

    public String getVideoID() {
        return videoID;
    }

    public void setVideoID(String videoID) {
        this.videoID = videoID;
    }

    public String[] getSegments() {
        return segments;
    }

    public void setSegments(String[] segments) {
        this.segments = segments;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    private String baseUrl;
    private int    height;

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    private int    width;
    private String fps;

    public YoutubeFinalLinkResource(/* Storable */) {
    }

    public YoutubeFinalLinkResource(YoutubeStreamData si) {
        itag = si.getItag();
        videoID = si.getClip().videoID;
        segments = si.getSegments();
        baseUrl = si.getUrl();
        height = si.getHeight();
        width = si.getWidth();
        fps = si.getFps();

    }

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    public YoutubeStreamData toStreamDataObject() {
        YoutubeStreamData ret = new YoutubeStreamData(null, new YoutubeClipData(videoID), baseUrl, itag, null);
        ret.setHeight(height);
        ret.setWidth(width);

        ret.setSegments(segments);
        return ret;
    }
}
