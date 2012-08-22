package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class HEAACAudioStream extends MpegAudioStream {

    public HEAACAudioStream() {
        super("audio/aacp");

        mpegVersions = new int[] { 2, 4 };

    }

}
