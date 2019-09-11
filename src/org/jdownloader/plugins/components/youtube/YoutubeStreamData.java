package org.jdownloader.plugins.components.youtube;

import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

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
    private String src;
    private long   contentLength = -1;
    private int    bitrate       = -1;

    public void setContentLength(long contentLength) {
        this.contentLength = contentLength;
    }

    public void setBitrate(int bitrate) {
        this.bitrate = bitrate;
    }

    public String getSrc() {
        return src;
    }

    public YoutubeStreamData(String src, final YoutubeClipData vid, String url, YoutubeITAG itag, UrlQuery query) {
        this.src = src;
        this.clip = vid;
        this.itag = itag;
        this.url = url;
        if (query != null) {
            try {
                final String cLenString = query.get("clen");
                if (cLenString != null) {
                    contentLength = Long.parseLong(cLenString);
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
            try {
                final String bitrateString = query.get("bitrate");
                if (bitrateString != null) {
                    bitrate = Integer.parseInt(bitrateString);
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
            String v = query.get("projection_type");
            projectionType = v == null ? -1 : Integer.parseInt(v);
        }
    }

    public long getContentLength() {
        return contentLength;
    }

    public int getBitrate() {
        return bitrate;
    }

    public int getProjectionType() {
        return projectionType;
    }

    public void setProjectionType(int projectionType) {
        this.projectionType = projectionType;
    }
}