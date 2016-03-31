package org.jdownloader.plugins.components.youtube;

public enum VideoContainer implements MediaQualityInterface {
    THREEGP(4, 1),
    FLV(1, 1),
    MP4(6, 1),
    WEBM(5, 1);
    private double rating = -1;

    private VideoContainer(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
