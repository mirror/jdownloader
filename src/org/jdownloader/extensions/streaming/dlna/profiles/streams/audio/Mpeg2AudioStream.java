package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class Mpeg2AudioStream extends AbstractMpegAudioStream {

    public Mpeg2AudioStream(int mpegVersion, int mpegAudioVersion) {
        super("audio/mpeg");

        this.mpegVersions = new int[] { mpegVersion };
        this.mpegAudioVersions = new int[] { mpegAudioVersion };
        this.layers = new int[] { 2 };
    }

}
