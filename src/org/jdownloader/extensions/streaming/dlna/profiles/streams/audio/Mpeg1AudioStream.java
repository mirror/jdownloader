package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class Mpeg1AudioStream extends AbstractMpegAudioStream {

    public Mpeg1AudioStream(int mpegVersion, int mpegAudioVersion) {
        super("audio/mpeg");
        setCodecNames("mp1");
        // testfile: http://samples.mplayerhq.hu/A-codecs/MP3/mpeg_layer1_audio.mpg

        this.mpegVersions = new int[] { mpegVersion };
        this.mpegAudioVersions = new int[] { mpegAudioVersion };
        this.layers = new int[] { 1 };
    }

}
