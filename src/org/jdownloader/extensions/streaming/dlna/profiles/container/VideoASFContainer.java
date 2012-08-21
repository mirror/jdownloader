package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;
import org.jdownloader.extensions.streaming.dlna.MimeType;

public class VideoASFContainer extends AbstractAudioVideoContainer {

    public VideoASFContainer() {
        super(Extensions.AUDIO_VIDEO_ASF);

        setName(MimeType.VIDEO_ASF.getLabel());

    }

}
