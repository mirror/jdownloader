package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioMp3Stream extends RawStream {
    public static final RawAudioMp3Stream INSTANCE = new RawAudioMp3Stream();

    protected RawAudioMp3Stream() {
        super(Extensions.AUDIO_MP3);
        setName("audio/mpeg");

    }
}
