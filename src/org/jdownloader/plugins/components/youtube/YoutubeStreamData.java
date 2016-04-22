package org.jdownloader.plugins.components.youtube;

import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

import jd.http.QueryInfo;

public class YoutubeStreamData {

    private YoutubeClipData clip;

    @Override
    public String toString() {
        return "Itag" + itag.getITAG();
    }

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
    private int height;

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

    public String getFps() {
        return fps;
    }

    public void setFps(String fps) {
        this.fps = fps;
    }

    private int    width;
    private String fps;
    private int    projectionType;
    private String qualityLabel;

    public YoutubeStreamData(final YoutubeClipData vid, String url, YoutubeITAG itag, QueryInfo query) {
        this.clip = vid;
        this.itag = itag;
        this.url = url;
        if (query != null) {
            this.qualityLabel = query.get("quality_label");
            if (qualityLabel == null) {
                qualityLabel = query.get("quality");
            }
            String v = query.get("projection_type");
            projectionType = v == null ? -1 : Integer.parseInt(v);

        }

    }

    public int getProjectionType() {
        return projectionType;
    }

    public void setProjectionType(int projectionType) {
        this.projectionType = projectionType;
    }
}