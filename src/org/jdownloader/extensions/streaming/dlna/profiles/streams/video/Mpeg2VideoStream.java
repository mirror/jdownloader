package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class Mpeg2VideoStream extends MpegVideoStream {

    public Mpeg2VideoStream() {

        mpegVersions = new int[] { 2 };

    }

    private String[] level;

    public Mpeg2VideoStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
