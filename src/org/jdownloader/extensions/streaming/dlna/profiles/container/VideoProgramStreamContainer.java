package org.jdownloader.extensions.streaming.dlna.profiles.container;

import org.jdownloader.extensions.streaming.dlna.Extensions;

public class VideoProgramStreamContainer extends AbstractAudioVideoContainer {

    protected boolean systemStream;
    protected int     packetsize;

    public VideoProgramStreamContainer(Extensions... extensions) {
        super(extensions);
        setFormatName("mpeg");
        setName("video/mpeg");
        this.systemStream = true;

        packetsize = 188;

    }
}
