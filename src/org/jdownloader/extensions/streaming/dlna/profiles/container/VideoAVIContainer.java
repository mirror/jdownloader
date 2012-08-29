package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoAVIContainer extends AbstractAudioVideoContainer {
    public static final VideoAVIContainer INSTANCE = new VideoAVIContainer();

    protected VideoAVIContainer() {
        super(Extensions.AUDIO_VIDEO_MKV);

        setFormatName("avi");
        setName("video/x-msvideo");

    }
}
