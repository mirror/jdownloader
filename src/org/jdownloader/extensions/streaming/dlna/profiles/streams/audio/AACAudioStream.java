package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

public class AACAudioStream extends MpegAudioStream {

    public AACAudioStream() {
        super("audio/vnd.dlna.adts");

        mpegVersions = new int[] { 2, 4 };
        getProfileTags().add("lc");

    }

}
