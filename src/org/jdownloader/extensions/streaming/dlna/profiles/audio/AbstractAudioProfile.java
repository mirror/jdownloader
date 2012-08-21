package org.jdownloader.extensions.streaming.dlna.profiles.audio;

import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;

public abstract class AbstractAudioProfile extends Profile {

    protected InternalAudioStream stream;

    public InternalAudioStream getStream() {
        return stream;
    }

    public AbstractAudioProfile(String id) {
        super(id);

    }

}
