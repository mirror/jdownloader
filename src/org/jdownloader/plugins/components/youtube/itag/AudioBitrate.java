package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;

public enum AudioBitrate implements IntegerInterface,LabelInterface {
    KBIT_128(128, 1000),
    KBIT_152(152, 1000),
    KBIT_160(160, 1000),
    KBIT_192(192, 1000),
    KBIT_256(256, 1000),

    KBIT_32(32, 1000),
    KBIT_48(48, 1000),
    KBIT_64(64, 1000),
    KBIT_96(96, 1000),

    KBIT_32_ESTIMATED(31, 1000),
    KBIT_12(12, 1000),
    KBIT_24(24, 1000);
    private double rating = -1;
    private int    kbit;

    private AudioBitrate(int kbit, double modifier) {
        this.rating = kbit / modifier;
        this.kbit = kbit;
    }

    public int getKbit() {
        return kbit;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabel() {

        return kbit + " kbit/s";
    }

    @Override
    public int getInt() {
        return kbit;
    }

}
