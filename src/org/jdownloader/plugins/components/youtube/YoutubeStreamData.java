package org.jdownloader.plugins.components.youtube;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.extmanager.LoggerFactory;
import org.appwork.utils.parser.UrlQuery;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public class YoutubeStreamData {
    private YoutubeClipData clip;

    @Override
    public String toString() {
        if (lngId != null) {
            return "Itag" + itag.getITAG() + "(" + itag + ")" + "(" + lngId + ")";
        }
        return "Itag" + itag.getITAG() + "(" + itag + ")";
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
    private int    projectionType         = -1;
    private String src;
    private long   contentLength          = -1;
    private long   estimatedContentLength = -1;
    private int    throttle               = -1;

    public String getLngId() {
        return lngId;
    }

    public void setLngId(String lngId) {
        this.lngId = lngId;
    }

    private String lngId = null;

    @Override
    public int hashCode() {
        return itag.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (!(obj instanceof YoutubeStreamData)) {
            return false;
        }
        final YoutubeStreamData o = (YoutubeStreamData) obj;
        if (!StringUtils.equals(getClip().videoID, o.getClip().videoID)) {
            return false;
        } else if (getItag() != o.getItag()) {
            return false;
        } else if (!StringUtils.equals(getLngId(), o.getLngId())) {
            return false;
        } else {
            return true;
        }
    }

    public int getThrottle() {
        return throttle;
    }

    public void setThrottle(int throttle) {
        this.throttle = throttle;
    }

    public void setEstimatedContentLength(long estimatedContentLength) {
        this.estimatedContentLength = estimatedContentLength;
    }

    private long approxDurationMs = -1;

    public long getApproxDurationMs() {
        return approxDurationMs;
    }

    public void setApproxDurationMs(long approxDurationMs) {
        this.approxDurationMs = approxDurationMs;
    }

    private int bitrate        = -1;
    private int averageBitrate = -1;

    public int getAverageBitrate() {
        return averageBitrate;
    }

    public void setAverageBitrate(int averageBitrate) {
        this.averageBitrate = Math.max(-1, bitrate);
    }

    public void setContentLength(long contentLength) {
        this.contentLength = Math.max(-1, contentLength);
    }

    public void setBitrate(int bitrate) {
        this.bitrate = Math.max(-1, bitrate);
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
                    setContentLength(Long.parseLong(cLenString));
                }
            } catch (Throwable e) {
                LoggerFactory.getDefaultLogger().log(e);
            }
            try {
                final String bitrateString = query.get("bitrate");
                if (bitrateString != null) {
                    setBitrate(Integer.parseInt(bitrateString));
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

    public long getEstimatedContentLength() {
        return estimatedContentLength;
    }

    public long estimatedContentLength() {
        long estimatedContentLength = getEstimatedContentLength();
        if (estimatedContentLength > 0) {
            return estimatedContentLength;
        }
        final long duration = getApproxDurationMs();
        if (duration > 0) {
            int bitrate = getAverageBitrate();
            if (bitrate <= 0) {
                bitrate = getBitrate();
            }
            if (bitrate > 0) {
                estimatedContentLength = (bitrate * (duration / 1000)) / 8;
                return estimatedContentLength;
            }
        }
        return -1;
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