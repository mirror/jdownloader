package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;

public enum VideoCodec implements LabelInterface,TooltipInterface {
    H263("AVC H.263", "H263", 25, 1),
    H264("AVC H.264", "H264", 40, 1),
    VP8("Google VP8", "VP8", 20, 1),
    VP9_WORSE_PROFILE_1("Google VP9 Low Quality Profile", "VP9 LQ", 29, 1),
    VP9("Google VP9", "VP9", 30, 1),
    VP9_BETTER_PROFILE_1("Google VP9 Medium Quality Profile", "VP9 MQ", 31, 1),
    VP9_BETTER_PROFILE_2("Google VP9 High Quality Profile", "VP9 HQ", 32, 1);
    private double rating = -1;
    private String label;
    private String labelLong;

    public String getLabel() {

        return label;
    }

    private VideoCodec(String labelLong, String label, double rating, double modifier) {
        this.rating = rating / modifier;
        this.label = label;
        this.labelLong = labelLong;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getLabelLong() {
        return labelLong;
    }

    @Override
    public String getTooltip() {
        return labelLong;
    }

}
