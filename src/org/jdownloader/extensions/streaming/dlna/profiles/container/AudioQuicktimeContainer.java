package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class AudioQuicktimeContainer extends AbstractAudioContainer {
    public static final AudioQuicktimeContainer INSTANCE = new AudioQuicktimeContainer();

    protected AudioQuicktimeContainer() {
        super(Extensions.AUDIO_VIDEO_QT, Extensions.AUDIO_VIDEO_MOV);
        setName("video/quicktime");
    }
}
