package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoQuicktimeContainer extends AbstractAudioVideoContainer {
    public static final VideoQuicktimeContainer INSTANCE = new VideoQuicktimeContainer();

    /**
     * Example: "nb_streams": 2, "format_name": "mov,mp4,m4a,3gp,3g2,mj2", "format_long_name": "QuickTime / MOV", "start_time": "0.000000",
     * "duration": "3.441667", "size": "6994173", "bit_rate": "16257640", "tags": { "major_brand": "qt  ", "minor_version": "537199360",
     * "compatible_brands": "qt  ", "creation_time": "2009-07-06 09:45:08" }
     * 
     * 
     * 
     * 
     */
    protected VideoQuicktimeContainer() {
        super(Extensions.AUDIO_VIDEO_MOV);
        // guessed
        setFormatName("mov,mp4,m4a,3gp,3g2,mj2");
        setName("video/quicktime");

    }
}
