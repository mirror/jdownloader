package org.jdownloader.extensions.streaming.mediaarchive;

import org.jdownloader.extensions.streaming.dlna.profiles.Profile;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.audio.InternalAudioStream;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.video.InternalVideoStream;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.AudioStream;
import org.jdownloader.extensions.streaming.mediaarchive.prepare.VideoStream;

public class ProfileMatch {

    private Profile             profile;
    private InternalVideoStream profileVideoStream;

    public Profile getProfile() {
        return profile;
    }

    public String toString() {
        return profile + "\r\n" + sampleAudioStream + "\r\n" + profileAudioStream + "\r\n" + sampleVideoStream + "\r\n" + profileVideoStream;
    }

    public void setProfile(Profile profile) {
        this.profile = profile;
    }

    public InternalVideoStream getProfileVideoStream() {
        return profileVideoStream;
    }

    public void setProfileVideoStream(InternalVideoStream profileVideoStream) {
        this.profileVideoStream = profileVideoStream;
    }

    public InternalAudioStream getProfileAudioStream() {
        return profileAudioStream;
    }

    public void setProfileAudioStream(InternalAudioStream profileAudioStream) {
        this.profileAudioStream = profileAudioStream;
    }

    public VideoStream getSampleVideoStream() {
        return sampleVideoStream;
    }

    public void setSampleVideoStream(VideoStream sampleVideoStream) {
        this.sampleVideoStream = sampleVideoStream;
    }

    public AudioStream getSampleAudioStream() {
        return sampleAudioStream;
    }

    public void setSampleAudioStream(AudioStream sampleAudioStream) {
        this.sampleAudioStream = sampleAudioStream;
    }

    private InternalAudioStream profileAudioStream;
    private VideoStream         sampleVideoStream;
    private AudioStream         sampleAudioStream;

    public ProfileMatch(Profile p, InternalVideoStream matchingProfileVs, InternalAudioStream matchingProfileAs, VideoStream matchingVs, AudioStream matchingAs) {
        this.profile = p;
        this.profileVideoStream = matchingProfileVs;
        this.profileAudioStream = matchingProfileAs;
        this.sampleVideoStream = matchingVs;
        this.sampleAudioStream = matchingAs;
    }

}
