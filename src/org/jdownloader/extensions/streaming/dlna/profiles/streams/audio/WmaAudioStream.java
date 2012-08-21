package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class WmaAudioStream extends InternalAudioStream {

    private int[] wmaVersions;

    public int[] getWmaVersions() {
        return wmaVersions;
    }

    public WmaAudioStream() {
        super("audio/x-ms-wma");

    }

    public InternalAudioStream setWmaVersions(int... versions) {
        wmaVersions = versions;
        return this;
    }
}
