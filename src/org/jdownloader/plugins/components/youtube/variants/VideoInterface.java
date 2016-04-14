package org.jdownloader.plugins.components.youtube.variants;

import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;

public interface VideoInterface {

    int getVideoWidth();

    VideoResolution getVideoResolution();

    VideoCodec getVideoCodec();

    int getVideoFrameRate();

    int getVideoHeight();

}
