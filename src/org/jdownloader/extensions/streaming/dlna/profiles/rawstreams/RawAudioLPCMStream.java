package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioLPCMStream extends RawStream {
    public static final RawAudioLPCMStream INSTANCE = new RawAudioLPCMStream();

    protected RawAudioLPCMStream() {

        super(Extensions.AUDIO_PCM, Extensions.AUDIO_LPCM, Extensions.AUDIO_WAV, Extensions.AUDIO_AIFF);
        setName("audio/x-private1-lpcm");

    }
}
