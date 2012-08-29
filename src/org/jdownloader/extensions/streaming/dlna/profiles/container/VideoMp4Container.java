package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMp4Container extends AbstractAudioVideoContainer {
    public static final VideoMp4Container INSTANCE = new VideoMp4Container();
    private String                        variant;

    /*
     * 
     * "nb_streams": 4, "format_name": "mov,mp4,m4a,3gp,3g2,mj2", "format_long_name": "QuickTime / MOV", "start_time": "0.000000",
     * "duration": "69.768333", "size": "4568041", "bit_rate": "523795", "tags": { "major_brand": "mp42", "minor_version": "0",
     * "compatible_brands": "mp42isom", "creation_time": "2003-02-20 13:20:55" }
     */
    protected VideoMp4Container() {
        super(Extensions.AUDIO_VIDEO_MP4);
        setName("video/quicktime");
        setFormatName("mov,mp4,m4a,3gp,3g2,mj2");
        variant = "iso";

    }
}
