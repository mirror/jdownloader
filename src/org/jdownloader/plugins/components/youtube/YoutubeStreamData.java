package org.jdownloader.plugins.components.youtube;

public class YoutubeStreamData {

    private YoutubeClipData clip;

    public void setClip(YoutubeClipData clip) {
        this.clip = clip;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setItag(YoutubeITAG itag) {
        this.itag = itag;
    }

    private String url;

    public YoutubeClipData getClip() {
        return clip;
    }

    public String getUrl() {
        return url;
    }

    private String[] segments;

    public String[] getSegments() {
        return segments;
    }

    public void setSegments(String[] segments) {
        this.segments = segments;
    }

    public YoutubeITAG getItag() {
        return itag;
    }

    YoutubeITAG itag;

    public YoutubeStreamData(final YoutubeClipData vid, String url, YoutubeITAG itag) {
        this.clip = vid;
        this.itag = itag;
        this.url = url;

    }
}