package org.jdownloader.plugins.components.youtube.variants;

public enum DownloadType {
    DASH_AUDIO,
    DASH_VIDEO,

    VIDEO,
    /**
     * Static videos have a static url in YT_STATIC_URL
     */
    IMAGE,
    SUBTITLES,
    DESCRIPTION,
    HLS_VIDEO,

}