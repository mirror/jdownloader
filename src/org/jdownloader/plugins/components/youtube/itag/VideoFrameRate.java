package org.jdownloader.plugins.components.youtube.itag;

public enum VideoFrameRate {
    FPS_60(60, 5, 100),
    FPS_30(30, 3, 100),

    FPS_6(6, -5, 100),
    FPS_15(15, -4, 100),
    FPS_24(24, -1, 100);

    private double rating = -1;
    private double fps;

    public double getFps() {
        return fps;
    }

    private VideoFrameRate(double fps, double rating, double modifier) {
        this.rating = rating / modifier;
        this.fps = fps;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabel() {

        return (int) Math.ceil(getFps()) + "fps";
    }
}
