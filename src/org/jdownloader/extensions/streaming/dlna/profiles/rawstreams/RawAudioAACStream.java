package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioAACStream extends RawStream {
    public static final RawAudioAACStream INSTANCE = new RawAudioAACStream();

    protected RawAudioAACStream() {
        super(Extensions.AUDIO_AAC, Extensions.AUDIO_ADTS);
    }
}
