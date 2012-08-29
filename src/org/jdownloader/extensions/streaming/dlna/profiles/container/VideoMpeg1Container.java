package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMpeg1Container extends AbstractAudioVideoContainer {
    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    /*
     * "nb_streams": 2, "format_name": "mpeg", "format_long_name": "MPEG-PS (MPEG-2 Program Stream)", "start_time": "0.489111", "duration":
     * "54.960000", "size": "3860484", "bit_rate": "561933"
     */
    public VideoMpeg1Container() {
        super(Extensions.AUDIO_VIDEO_MPG, Extensions.AUDIO_VIDEO_MPEG, Extensions.AUDIO_VIDEO_MPE, Extensions.AUDIO_VIDEO_M1V);

        setFormatName("mpeg");
        setName("video/mpeg");
        this.systemStream = true;
        this.mpegVersions = new int[] { 1 };
    }

}
