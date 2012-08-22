package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class AVCStream extends Mpeg4VideoStream {

    public AVCStream() {
        setContentType("video/x-h264");
        profileTags = new String[] { "constrained-baseline" };
        mpegVersions = new int[] { 4 };
        systemStream = false;

    }

    private String[] level;

    public AVCStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
