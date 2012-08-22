package org.jdownloader.extensions.streaming.dlna.profiles.video;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;
import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;

public abstract class AbstractAudioVideoProfile extends Profile {
    private List<IntRange> systemBitrates;

    public AbstractAudioVideoProfile(String id) {
        super(id);
        audioStreams = new ArrayList<InternalAudioStream>();
        videoStreams = new ArrayList<InternalVideoStream>();
        systemBitrates = new ArrayList<IntRange>();
    }

    protected void addVideoStream(InternalVideoStream stream) {
        videoStreams.add(stream);
    }

    protected void addAudioStream(InternalAudioStream stream) {
        audioStreams.add(stream);
    }

    public void addSystemBitrateRange(int min, int max) {
        systemBitrates.add(new IntRange(min, max));

    }

    private List<InternalAudioStream> audioStreams;
    private List<InternalVideoStream> videoStreams;

    public List<InternalAudioStream> getAudioStreams() {
        return audioStreams;
    }

    public List<InternalVideoStream> getVideoStreams() {
        return videoStreams;
    }

}
