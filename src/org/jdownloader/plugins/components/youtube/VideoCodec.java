package org.jdownloader.plugins.components.youtube;

public enum VideoCodec {
    H263("H263", 25, 1),
    H264("H264", 40, 1),
    VP8("VP8", 20, 1),
    VP9_WORSE_PROFILE_1("VP9 Low Quality Profile", 29, 1),
    VP9("VP9", 30, 1),
    VP9_BETTER_PROFILE_1("VP9 Medium Quality Profile", 31, 1),
    VP9_BETTER_PROFILE_2("VP9 High Quality Profile", 32, 1);
    private double rating = -1;
    private String label;

    public String getLabel(Object caller) {

        return label;
    }

    private VideoCodec(String label, double rating, double modifier) {
        this.rating = rating / modifier;
        this.label = label;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
