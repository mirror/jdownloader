package org.jdownloader.plugins.components.youtube;

public enum VideoResolution implements MediaQualityInterface {
    P_1080(1080, 1),
    P_144(144, 1),
    P_1440(1440, 1),
    P_2160(2160, 1),
    P_2160_ESTIMATED(2160 - 1, 1),
    P_240(240, 1),
    P_270(270, 1),
    P_360(360, 1),
    P_480(480, 1),
    P_720(720, 1),
    P_4320(4320, 1),
    P_72(72, 1);
    private double rating = -1;

    private VideoResolution(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
