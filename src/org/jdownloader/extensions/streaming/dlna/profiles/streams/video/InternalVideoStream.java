package org.jdownloader.extensions.streaming.dlna.profiles.streams.video;

import java.util.ArrayList;
import java.util.Arrays;
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

    public String toString() {
        return this.getContentType() + "(" + Arrays.toString(this.getCodecNames()) + ") Bitrates:" + bitrates + " Resolutions:" + resolutions + " Framerates: " + frameRates + " PAR:" + pixelAspectRatios;
    }

    public InternalVideoStream setProfileTags(String... tags) {
        super.setProfileTags(tags);
        return this;
    }

    public InternalStream addProfileTags(String... tags) {
        super.addProfileTags(tags);
        return this;
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

    public boolean checkBitrate(int bitrate) {
        if (bitrates.size() == 0) return true;
        for (IntRange r : bitrates) {
            if (r.contains(bitrate)) return true;
        }
        return false;
    }

    public boolean checkFrameRate(int[] frameRate) {
        if (frameRates.size() == 0) return true;
        if (frameRate == null) return false;
        if (frameRate.length != 2) throw new IllegalArgumentException("Framerate must be int[2]");
        for (FrameRate i : frameRates) {
            if (i.matches(frameRate[0], frameRate[1])) return true;

        }
        return false;
    }

    public boolean checkResolution(int width, int height) {
        if (resolutions.size() == 0) return true;
        if (width <= 0 || height <= 0) return false;
        for (Resolution i : resolutions) {
            if (i.matches(width, height)) return true;

        }
        return false;
    }

    public boolean checkPixelAspectRatio(int[] pixelAspectRatio) {
        if (pixelAspectRatios.size() == 0) return true;
        if (pixelAspectRatio == null || pixelAspectRatio[0] == 0 || pixelAspectRatio[1] == 0) return false;
        if (pixelAspectRatio.length != 2) throw new IllegalArgumentException("pixelAspectRatio must be int[2]");
        for (Break i : pixelAspectRatios) {
            if (i.equals(pixelAspectRatio[0], pixelAspectRatio[1])) return true;

        }
        return false;
    }

}
