package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoFLVContainer extends AbstractAudioVideoContainer {
    public static final VideoFLVContainer INSTANCE = new VideoFLVContainer();

    protected VideoFLVContainer() {
        super(Extensions.AUDIO_VIDEO_FLV);
        setFormatName("flv");
        setName("application/x-flv");

    }
}
