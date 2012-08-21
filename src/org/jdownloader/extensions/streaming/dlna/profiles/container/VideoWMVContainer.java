package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.MimeType;

public class VideoWMVContainer extends AbstractAudioVideoContainer {

    public VideoWMVContainer() {
        super(Extensions.AUDIO_VIDEO_WMV);

        setName(MimeType.VIDEO_WMV.getLabel());

    }

    private String[] level;

    public VideoWMVContainer setLevel(String[] strings) {
        this.level = strings;
        return this;
    }
}
