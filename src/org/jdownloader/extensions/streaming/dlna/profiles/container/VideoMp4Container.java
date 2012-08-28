package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMp4Container extends AbstractAudioVideoContainer {
    public static final VideoMp4Container INSTANCE = new VideoMp4Container();
    private String                        variant;

    protected VideoMp4Container() {
        super(Extensions.AUDIO_VIDEO_MP4);
        setName("video/quicktime");

        variant = "iso";

    }
}
