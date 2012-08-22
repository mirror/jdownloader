package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class Mp3AudioStream extends MpegAudioStream {

    public Mp3AudioStream() {
        super("audio/mp3");
        mpegVersions = new int[] { 1 };
        layers = new int[] { 3 };

    }

}
