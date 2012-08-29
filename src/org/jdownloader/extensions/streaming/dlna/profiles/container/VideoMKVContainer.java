package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMKVContainer extends AbstractAudioVideoContainer {
    public static final VideoMKVContainer INSTANCE = new VideoMKVContainer();

    protected VideoMKVContainer() {
        super(Extensions.AUDIO_VIDEO_MKV);

        setFormatName("matroska,webm");
        setName("video/x-matroska");

    }
}
