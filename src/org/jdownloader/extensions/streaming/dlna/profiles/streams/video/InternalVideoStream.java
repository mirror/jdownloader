package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

import java.util.ArrayList;
import java.util.List;

import org.jdownloader.extensions.streaming.dlna.profiles.IntRange;
import org.jdownloader.extensions.streaming.dlna.profiles.streams.InternalStream;
import org.jdownloader.extensions.streaming.dlna.profiles.video.FrameRate;
import org.jdownloader.extensions.streaming.dlna.profiles.video.Resolution;

public class InternalVideoStream extends InternalStream {
    private List<IntRange>   bitrates;
    private List<Resolution> resolutions;
    private List<FrameRate>  frameRates;
    private List<Break>      pixelAspectRatios;

    public InternalVideoStream(String contentType) {
        super(contentType);
        bitrates = new ArrayList<IntRange>();
        resolutions = new ArrayList<Resolution>();
        frameRates = new ArrayList<FrameRate>();
        pixelAspectRatios = new ArrayList<Break>();
    }

    public InternalVideoStream setProfileTags(String[] profileTags) {
        this.profileTags = profileTags;
        return this;
    }

    public InternalVideoStream setSystemStream(boolean systemStream) {
        this.systemStream = systemStream;
        return this;
    }

    public InternalVideoStream addBitrateRange(int min, int max) {
        bitrates.add(new IntRange(min, max));
        return this;
    }

    public InternalVideoStream addFrameRate(FrameRate framerate) {
        frameRates.add(framerate);
        return this;
    }

    public InternalVideoStream addResolution(Resolution resolution) {
        resolutions.add(resolution);
        return this;
    }

    public void addResolution(int width, int height) {
        resolutions.add(Resolution.get(width, height));
    }

    public void addPixelAspectRatio(int counter, int denominator) {
        pixelAspectRatios.add(new Break(counter, denominator));
    }

}
