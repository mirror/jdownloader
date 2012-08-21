package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMpeg1Container extends AbstractAudioVideoContainer {
    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    public VideoMpeg1Container() {
        super(Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M1V);
        setName("video/mpeg");
        this.systemStream = true;
        this.mpegVersions = new int[] { 1 };
    }

}
