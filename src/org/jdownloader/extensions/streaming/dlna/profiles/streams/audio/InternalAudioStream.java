package org.jdownloader.extensions.streaming.dlna.profiles.streams.audio;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.InternalStream;

public class InternalAudioStream extends InternalStream {

    private List<IntRange> channels;
    private List<IntRange> samplingRates;
    private List<IntRange> bitrates;

    public InternalAudioStream(String contentType) {
        super(contentType);

        bitrates = new ArrayList<IntRange>();
        samplingRates = new ArrayList<IntRange>();
        channels = new ArrayList<IntRange>();
    }

    public InternalAudioStream setProfileTags(String[] profileTags) {
        super.setProfileTags(profileTags);
        return this;
    }

    public InternalAudioStream addBitrateRange(int min, int max) {
        bitrates.add(new IntRange(min, max));
        return this;
    }

    public InternalAudioStream addSamplingRateRange(int min, int max) {
        samplingRates.add(new IntRange(min, max));
        return this;
    }

    public InternalAudioStream addChannelRange(int min, int max) {
        channels.add(new IntRange(min, max));
        return this;
    }

    public InternalAudioStream addSamplingRates(int... rates) {
        for (int i : rates) {
            addSamplingRateRange(i, i);
        }
        return this;
    }

}
