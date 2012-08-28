package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoQuicktimeContainer extends AbstractAudioVideoContainer {
    public static final VideoQuicktimeContainer INSTANCE = new VideoQuicktimeContainer();

    protected VideoQuicktimeContainer() {
        super(Extensions.AUDIO_VIDEO_MOV);
        setName("video/quicktime");

    }
}
