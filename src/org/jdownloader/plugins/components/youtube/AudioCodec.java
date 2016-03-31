package org.jdownloader.plugins.components.youtube;

public enum AudioCodec implements MediaQualityInterface {
    AAC(4, 10000),
    MP3(2, 10000),
    OPUS(1, 10000),
    VORBIS(3, 10000),
    AAC_M4A(5, 10000),
    AMRNB(1, 10001);

    private double rating = -1;

    private AudioCodec(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
