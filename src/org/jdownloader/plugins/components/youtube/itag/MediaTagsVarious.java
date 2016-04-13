package org.jdownloader.plugins.components.youtube.itag;

public enum MediaTagsVarious {

    SUBTITLE(1, 10),
    DESCRIPTION(1, 10);

    private double rating = -1;

    private MediaTagsVarious(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public double getRating() {
        return rating;
    }
}
