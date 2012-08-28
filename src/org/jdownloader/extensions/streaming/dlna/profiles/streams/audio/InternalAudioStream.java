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

    public InternalAudioStream addBitrateRange(int min, int max) {
        bitrates.add(new IntRange(min, max));
        return this;
    }

    public InternalAudioStream setProfileTags(String... tags) {
        super.setProfileTags(tags);
        return this;
    }

    public InternalAudioStream addProfileTags(String... tags) {
        super.addProfileTags(tags);
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

    public boolean checkSamplingRate(int samplingRate) {
        if (samplingRates.size() == 0) return true;
        for (IntRange i : samplingRates) {
            if (i.contains(samplingRate)) return true;
        }
        return false;
    }

    public boolean checkChannels(int channelNum) {
        if (channels.size() == 0) return true;
        for (IntRange i : channels) {
            if (i.contains(channelNum)) return true;
        }
        return false;
    }

    public boolean checkBitrate(int bitrate) {
        if (bitrates.size() == 0) return true;
        for (IntRange i : bitrates) {
            if (i.contains(bitrate)) return true;
        }
        return false;
    }

}
