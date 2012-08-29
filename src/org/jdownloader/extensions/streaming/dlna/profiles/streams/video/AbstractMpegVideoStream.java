package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

public abstract class AbstractMpegVideoStream extends InternalVideoStream {
    public AbstractMpegVideoStream() {
        super("video/mpeg");
    }

    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    public AbstractMpegVideoStream setMpegVersions(int[] mpegVersions) {
        this.mpegVersions = mpegVersions;
        return this;
    }

}
