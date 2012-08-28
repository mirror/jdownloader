package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class AVCStream extends Mpeg4VideoStream {
    public static final String BASELINE_CONSTRAINED = "Baseline Constrained";

    public AVCStream() {
        setContentType("video/x-h264");

        getProfileTags().add(BASELINE_CONSTRAINED);
        mpegVersions = new int[] { 4 };
        systemStream = false;

    }

    private String[] level;

    public AVCStream setLevel(String[] strings) {
        this.level = strings;
        return this;
    }

}
