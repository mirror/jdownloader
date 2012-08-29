package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class Mpeg2VideoStream extends AbstractMpegVideoStream {

    public Mpeg2VideoStream() {
        setCodecNames("mpeg2video");
        mpegVersions = new int[] { 4 };

    }

    private String[] level;

    public Mpeg2VideoStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
