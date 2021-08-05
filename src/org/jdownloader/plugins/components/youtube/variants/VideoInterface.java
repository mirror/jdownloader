package org.jdownloader.plugins.components.youtube.variants;

import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;

public interface VideoInterface {
    int getVideoWidth();

    VideoResolution getVideoResolution();

    VideoCodec getVideoCodec();

    int getVideoFrameRate();

    int getVideoHeight();

    YoutubeITAG getVideoITAG();
}
