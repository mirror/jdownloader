package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class Mpeg3AudioStream extends MpegAudioStream {

    public Mpeg3AudioStream() {
        super("audio/mp3");
        mpegVersions = new int[] { 1 };
        layers = new int[] { 3 };

    }

}
