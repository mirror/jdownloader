package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public class MpegVideoStream extends InternalVideoStream {
    public MpegVideoStream() {
        super("video/mpeg");
    }

    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    public MpegVideoStream setMpegVersions(int[] mpegVersions) {
        this.mpegVersions = mpegVersions;
        return this;
    }

}
