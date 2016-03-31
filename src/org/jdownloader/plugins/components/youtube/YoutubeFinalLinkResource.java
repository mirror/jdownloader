package org.jdownloader.plugins.components.youtube;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;

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

    public YoutubeFinalLinkResource(/* Storable */) {
    }

    public YoutubeFinalLinkResource(YoutubeStreamData si) {
        itag = si.getItag();
        videoID = si.getClip().videoID;
        segments = si.getSegments();
        baseUrl = si.getUrl();

    }

    public YoutubeStreamData toStreamDataObject() {
        YoutubeStreamData ret = new YoutubeStreamData(new YoutubeClipData(videoID), baseUrl, itag);
        ret.setSegments(segments);
        return ret;
    }
}
