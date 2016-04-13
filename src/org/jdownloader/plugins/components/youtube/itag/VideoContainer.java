package org.jdownloader.plugins.components.youtube.itag;

public enum VideoContainer {
    THREEGP("3gp", 4, 1),
    FLV("Flv", 1, 1),
    MP4("Mp4", 6, 1),
    WEBM("WebM", 5, 1);
    private double rating = -1;
    private String label;

    private VideoContainer(String label, double rating, double modifier) {
        this.rating = rating / modifier;
        this.label = label;
    }

    public String getLabel(Object caller) {
        return label;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
