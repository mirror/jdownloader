package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioAc3Stream extends RawStream {
    public static final RawAudioAc3Stream INSTANCE = new RawAudioAc3Stream();

    protected RawAudioAc3Stream() {
        super(Extensions.AUDIO_VIDEO_AC3);
        setName("audio/x-ac3");

    }
}
