package org.jdownloader.extensions.streaming.mediaarchive.prepare.ffmpeg;

import org.appwork.storage.Storable;

public class Format implements Storable {
    private Format(/* STorable */) {

    }

    private Tags tags;

    public Tags getTags() {
        return tags;
    }

    public void setTags(Tags tags) {
        this.tags = tags;
    }

    private int nb_streams;

    public int getNb_streams() {
        return nb_streams;
    }

    public void setNb_streams(int nb_streams) {
        this.nb_streams = nb_streams;
    }

    public String getFormat_name() {
        return format_name;
    }

    public void setFormat_name(String format_name) {
        this.format_name = format_name;
    }

    public String getFormat_long_name() {
        return format_long_name;
    }

    public void setFormat_long_name(String format_long_name) {
        this.format_long_name = format_long_name;
    }

    public String getDuration() {
        return duration;
    }

    public void setDuration(String duration) {
        this.duration = duration;
    }

    public String getSize() {
        return size;
    }

    public void setSize(String size) {
        this.size = size;
    }

    public String getBit_rate() {
        return bit_rate;
    }

    public void setBit_rate(String bit_rate) {
        this.bit_rate = bit_rate;
    }

    public int parseDuration() {
        try {
            return (int) Double.parseDouble(getDuration());
        } catch (Throwable e) {
            return -1;
        }
    }

    public int parseBitrate() {
        try {
            return (int) Integer.parseInt(getBit_rate());
        } catch (Throwable e) {
            return -1;
        }
    }

    private String format_name;
    private String format_long_name;
    private String duration;
    private String size;
    private String bit_rate;

    public long parseSize() {
        try {
            return (long) Long.parseLong(getSize());
        } catch (Throwable e) {
            return -1;
        }
    }
}
