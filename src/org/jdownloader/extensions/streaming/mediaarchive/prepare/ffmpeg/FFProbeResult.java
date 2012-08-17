package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import org.appwork.storage.Storable;

public class FFProbeResult implements Storable {
    public FFProbeResult(/* Storable */) {

    }

    private String codec_name;

    public String getCodec_name() {
        return codec_name;
    }

    public void setCodec_name(String codec_name) {
        this.codec_name = codec_name;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public String getCodec_type() {
        return codec_type;
    }

    public void setCodec_type(String codec_type) {
        this.codec_type = codec_type;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getBit_rate() {
        return bit_rate;
    }

    public void setBit_rate(String bit_rate) {
        this.bit_rate = bit_rate;
    }

    private int    index;
    private int    width;
    private int    height;
    private String codec_type;

    private String duration;
    private String bit_rate;

}
