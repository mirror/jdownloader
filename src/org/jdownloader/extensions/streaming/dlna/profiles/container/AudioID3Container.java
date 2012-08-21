package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class AudioID3Container extends AbstractAudioContainer {
    public static final AudioID3Container INSTANCE = new AudioID3Container();

    protected AudioID3Container() {
        super(Extensions.AUDIO_ID3);
        setName("application/x-id3");

    }
}
