package org.jdownloader.extensions.streaming.dlna.profiles.rawstreams;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class RawAudioEac3Stream extends RawStream {
    public static final RawAudioEac3Stream INSTANCE = new RawAudioEac3Stream();

    protected RawAudioEac3Stream() {
        super(Extensions.AUDIO_VIDEO_AC3);
        setName("audio/eac3");

    }
}
