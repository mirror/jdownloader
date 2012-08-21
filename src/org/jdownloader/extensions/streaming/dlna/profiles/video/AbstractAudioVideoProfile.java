package org.jdownloader.extensions.streaming.dlna.profiles.video;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;

public abstract class AbstractAudioVideoProfile extends Profile {

    public AbstractAudioVideoProfile(String id) {
        super(id);
        audioStreams = new ArrayList<InternalAudioStream>();
        videoStreams = new ArrayList<InternalVideoStream>();
    }

    protected void addVideoStream(InternalVideoStream stream) {
        videoStreams.add(stream);
    }

    protected void addAudioStream(InternalAudioStream stream) {
        audioStreams.add(stream);
    }

    private List<InternalAudioStream> audioStreams;

    public List<InternalAudioStream> getAudioStreams() {
        return audioStreams;
    }

    public List<InternalVideoStream> getVideoStreams() {
        return videoStreams;
    }

    private List<InternalVideoStream> videoStreams;

}
