package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMpegTsContainer extends AbstractAudioVideoContainer {
    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    protected boolean systemStream;
    protected int     packetsize;

    public boolean isSystemStream() {
        return systemStream;
    }

    public VideoMpegTsContainer() {
        super(Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M2V, Extensions.AUDIO_VIDEO_MP2P, Extensions.AUDIO_VIDEO_MP2T, Extensions.AUDIO_VIDEO_TS, Extensions.AUDIO_VIDEO_PS, Extensions.AUDIO_VIDEO_PES);

        setName("video/mpegts");
        this.systemStream = true;
        this.mpegVersions = new int[] { 1 };
        packetsize = 188;
    }

}
