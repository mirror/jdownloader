package org.jdownloader.plugins.components.youtube.itag;

public enum VideoResolution {
    P_1080(1920, 1080, 1080, 1),
    P_144(256, 144, 144, 1),
    P_1440(2560, 1440, 1440, 1),
    P_2160(3840, 2160, 2160, 1),
    P_2160_ESTIMATED(3840, 2160, 2160 - 1, 1),
    P_240(352, 240, 240, 1),
    P_270(480, 270, 270, 1),
    P_360(480, 360, 360, 1),
    P_480(640, 480, 480, 1),
    P_720(1280, 720, 720, 1),
    P_4320(7680, 4320, 4320, 1),
    P_72(128, 72, 72, 1),
    P_1920(1080, 1920, 1920, 1);
    private double rating = -1;
    private int    height;
    private int    width;

    private VideoResolution(int width, int height, double rating, double modifier) {
        this.rating = rating / modifier;
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

}
