package org.jdownloader.plugins.components.youtube;

public enum ImageQuality implements MediaQualityInterface {
    HIGH(3, 10),
    LOW(1, 10),
    HIGHEST(4, 10),
    NORMAL(2, 10);
    private double rating = -1;

    private ImageQuality(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
