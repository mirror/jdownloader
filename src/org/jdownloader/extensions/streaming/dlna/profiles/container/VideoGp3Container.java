package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoGp3Container extends AbstractAudioVideoContainer {
    public static final VideoGp3Container INSTANCE = new VideoGp3Container();

    /*
     * "format": { "filename": "http://192.168.2.122:3128/stream/ffmpeg/1346240942613", "nb_streams": 1, "format_name":
     * "mov,mp4,m4a,3gp,3g2,mj2", "format_long_name": "QuickTime / MOV", "start_time": "0.000000", "duration": "7.848333", "size": "32896",
     * "bit_rate": "33531", "tags": { "major_brand": "3gp6", "minor_version": "0", "compatible_brands": "3gp6isom3g2amp42", "creation_time":
     * "2007-07-18 09:45:08" } }
     */
    protected VideoGp3Container() {
        super(Extensions.AUDIO_VIDEO_GP3);

        setName("application/x-3gp");

    }
}
