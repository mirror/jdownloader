package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class AudioMp4Container extends AbstractAudioContainer {
    public static final AudioMp4Container INSTANCE = new AudioMp4Container();

    protected AudioMp4Container() {
        super(Extensions.AUDIO_VIDEO_MP4, Extensions.AUDIO_M4A);
        setName("video/quicktime");

    }
}
