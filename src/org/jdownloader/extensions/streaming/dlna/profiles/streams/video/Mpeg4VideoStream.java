package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class Mpeg4VideoStream extends MpegVideoStream {

    public Mpeg4VideoStream() {

        mpegVersions = new int[] { 4 };
        systemStream = false;
    }

    private String[] level;

    public Mpeg4VideoStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
