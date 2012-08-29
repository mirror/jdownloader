package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class Mp3AudioStream extends AbstractMpegAudioStream {

    public Mp3AudioStream() {
        super("audio/mp3");
        setCodecNames("mp3");
        // http://samples.mplayerhq.hu/3D/car.avi
        setCodecTags("U[0][0][0]");
        mpegVersions = new int[] { 1 };
        layers = new int[] { 3 };

    }

}
