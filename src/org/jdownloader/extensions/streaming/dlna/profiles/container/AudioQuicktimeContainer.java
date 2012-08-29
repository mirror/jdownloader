package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class AudioQuicktimeContainer extends AbstractAudioContainer {
    public static final AudioQuicktimeContainer INSTANCE = new AudioQuicktimeContainer();

    /*
     * * "filename": "http://192.168.2.122:3128/stream/ffmpeg/1346240942625", "nb_streams": 1, "format_name": "mov,mp4,m4a,3gp,3g2,mj2",
     * "format_long_name": "QuickTime / MOV", "start_time": "0.000000", "duration": "12.816667", "size": "52387", "bit_rate": "32699",
     * "tags": { "creation_time": "2002-06-02 18:51:39" }
     */
    protected AudioQuicktimeContainer() {
        super(Extensions.AUDIO_VIDEO_QT, Extensions.AUDIO_VIDEO_MOV);
        setFormatName("mov,mp4,m4a,3gp,3g2,mj2");
        setName("video/quicktime");
    }
}
