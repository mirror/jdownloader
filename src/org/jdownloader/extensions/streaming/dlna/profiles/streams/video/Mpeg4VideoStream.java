package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class Mpeg4VideoStream extends AbstractMpegVideoStream {

    public Mpeg4VideoStream() {
        // testfile: http://samples.mplayerhq.hu/A-codecs/AAC/Bandit.mp4
        setCodecNames("mpeg4");
        setCodecTags("mp4v");
        mpegVersions = new int[] { 4 };
        systemStream = false;
    }

    private String[] level;

    public Mpeg4VideoStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
