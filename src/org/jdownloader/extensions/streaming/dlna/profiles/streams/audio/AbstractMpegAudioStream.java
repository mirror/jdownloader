package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public abstract class AbstractMpegAudioStream extends InternalAudioStream {

    protected AbstractMpegAudioStream(String contentType) {
        super(contentType == null ? "audio/mpeg" : contentType);
    }

    protected int[] mpegVersions;

    public int[] getMpegVersions() {
        return mpegVersions;
    }

    public AbstractMpegAudioStream setLayers(int[] layers) {
        this.layers = layers;
        return this;
    }

    public int[] getMpegAudioVersions() {
        return mpegAudioVersions;
    }

    public int[] getLayers() {
        return layers;
    }

    protected int[] mpegAudioVersions;
    protected int[] layers;

}
