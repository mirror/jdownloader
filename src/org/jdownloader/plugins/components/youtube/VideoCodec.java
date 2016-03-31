package org.jdownloader.plugins.components.youtube;

public enum VideoCodec implements MediaQualityInterface {
    H263(25, 1),
    H264(40, 1),
    VP8(20, 1),
    VP9_WORSE_PROFILE_1(29, 1),
    VP9(30, 1),
    VP9_BETTER_PROFILE_1(31, 1),
    VP9_BETTER_PROFILE_2(32, 1);
    private double rating = -1;

    private VideoCodec(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
