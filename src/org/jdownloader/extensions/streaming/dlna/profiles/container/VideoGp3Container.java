package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoGp3Container extends AbstractAudioContainer {
    public static final VideoGp3Container INSTANCE = new VideoGp3Container();

    protected VideoGp3Container() {
        super(Extensions.AUDIO_VIDEO_GP3);
        setName("application/x-3gp");

    }
}
