package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class AudioWmaAsfContainer extends AbstractAudioContainer {
    public static final AudioWmaAsfContainer INSTANCE = new AudioWmaAsfContainer();

    protected AudioWmaAsfContainer() {
        super(Extensions.AUDIO_ASF);
        setName("video/x-ms-asf");

    }
}
