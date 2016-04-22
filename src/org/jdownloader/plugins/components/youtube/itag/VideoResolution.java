package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.IntegerInterface;
import org.appwork.storage.config.annotations.LabelInterface;

public enum VideoResolution implements LabelInterface,IntegerInterface {
    P_1080(1920, 1080),
    P_144(256, 144),
    P_180(320, 180),
    P_90(120, 90),
    P_1440(2560, 1440),
    P_2160(3840, 2160),
    // P_2160_ESTIMATED(3840, 2160, 2160 - 1, 1) {
    // @Override
    // public String getLabel() {
    // return "~" + super.getLabel();
    // }
    // },
    P_240(352, 240),
    P_270(480, 270),
    P_360(480, 360),
    P_480(640, 480),
    P_720(1280, 720),
    P_4320(7680, 4320),
    P_72(128, 72),
    P_1920(1080, 1920);
    private double rating = -1;
    private int    height;
    private int    width;

    private VideoResolution(int width, int height) {
        this.rating = Math.min(width, height);
        this.height = height;
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabel() {

        return height + "p";
    }

    public int getWidth() {
        return width;
    }

    @Override
    public int getInt() {
        return height;
    }

    public static VideoResolution getByHeight(int height) {
        for (VideoResolution r : values()) {
            if (r.getHeight() == height) {
                return r;
            }
        }
        return null;
    }

}
