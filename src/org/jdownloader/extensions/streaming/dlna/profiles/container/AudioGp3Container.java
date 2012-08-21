package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class AudioGp3Container extends AbstractAudioContainer {
    protected AudioGp3Container() {
        super(Extensions.AUDIO_VIDEO_GP3);
        setName("application/x-3gp");
    }

    public static final AudioGp3Container INSTANCE = new AudioGp3Container();
}
