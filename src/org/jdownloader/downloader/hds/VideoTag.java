package org.jdownloader.downloader.hds;

public class VideoTag {
    // CodecID UB [4] Codec Identifier. The following values are defined:
    // 2 = Sorenson H.263
    public static final int SORENSEN_H263   = 2;
    // 3 = Screen video
    public static final int SCREEN_VIDEO    = 3;
    // 4 = On2 VP6
    public static final int VP6             = 4;
    // 5 = On2 VP6 with alpha channel
    public static final int VP6_ALPHA       = 5;
    // 6 = Screen video version 2
    public static final int SCREEN_VIDEO_V2 = 6;
    // 7 = AVC
    public static final int AVC             = 7;
}
