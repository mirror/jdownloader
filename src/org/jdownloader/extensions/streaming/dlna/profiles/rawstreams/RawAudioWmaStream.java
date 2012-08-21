package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioWmaStream extends RawStream {
    public static final RawAudioWmaStream INSTANCE = new RawAudioWmaStream();

    protected RawAudioWmaStream() {
        super(Extensions.AUDIO_WMA);
        setName("audio/x-wma");

    }
}
