package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioAMRStream extends RawStream {
    public static final RawAudioAMRStream INSTANCE = new RawAudioAMRStream();

    protected RawAudioAMRStream() {
        super(Extensions.AUDIO_AMR);
        setName("audio/AMR");
    }
}
