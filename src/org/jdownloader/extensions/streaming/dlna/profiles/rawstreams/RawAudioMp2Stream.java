package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioMp2Stream extends RawStream {
    public static final RawAudioMp2Stream INSTANCE = new RawAudioMp2Stream();

    protected RawAudioMp2Stream() {
        super(Extensions.AUDIO_MP2);
        setName("audio/mpeg");

    }
}
