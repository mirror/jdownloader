package org.jdownloader.plugins.components.youtube.itag;

public enum StreamContainer {
    DASH_AUDIO,
    DASH_VIDEO,
    PLAIN,
    FLV,
    THREEGP,
    MP4,
    WEBM;

    public double getRating() {
        return 0;
    }

}
