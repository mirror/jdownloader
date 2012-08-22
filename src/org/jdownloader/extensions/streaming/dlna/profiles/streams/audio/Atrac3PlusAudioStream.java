package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

import org.jdownloader.extensions.streaming.dlna.MimeType;

public class Atrac3PlusAudioStream extends MpegAudioStream {

    public Atrac3PlusAudioStream() {
        super(MimeType.AUDIO_ATRAC.getLabel());

        mpegVersions = new int[] { 2, 4 };

    }

}
