package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoMKVContainer extends AbstractAudioContainer {
    public static final VideoMKVContainer INSTANCE = new VideoMKVContainer();

    protected VideoMKVContainer() {
        super(Extensions.AUDIO_VIDEO_MKV);
        setName("video/x-matroska");

    }
}
