package org.jdownloader.plugins.components.youtube.itag;

public enum AudioCodec {
    AAC("AAC", 4, 10000),
    MP3("MP3", 2, 10000),
    OPUS("Opus", 1, 10000),
    VORBIS("Vorbis", 3, 10000),
    AMR("ARM", 1, 10001);

    private double rating = -1;
    private String label;

    private AudioCodec(String label, double rating, double modifier) {
        this.rating = rating / modifier;
        this.label = label;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabel() {
        return label;
    }

}
