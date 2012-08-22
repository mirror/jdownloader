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

    public List<IntRange> getBitrates() {
        return bitrates;
    }

    public void setBitrates(List<IntRange> bitrates) {
        this.bitrates = bitrates;
    }

    public List<Resolution> getResolutions() {
        return resolutions;
    }

    public void setResolutions(List<Resolution> resolutions) {
        this.resolutions = resolutions;
    }

    public List<FrameRate> getFrameRates() {
        return frameRates;
    }

    public void setFrameRates(List<FrameRate> frameRates) {
        this.frameRates = frameRates;
    }

    public List<Break> getPixelAspectRatios() {
        return pixelAspectRatios;
    }

    public void setPixelAspectRatios(List<Break> pixelAspectRatios) {
        this.pixelAspectRatios = pixelAspectRatios;
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

    public InternalVideoStream addResolution(int width, int height) {
        resolutions.add(Resolution.get(width, height));
        return this;
    }

    public InternalVideoStream addPixelAspectRatio(int counter, int denominator) {
        pixelAspectRatios.add(new Break(counter, denominator));
        return this;
    }

}
