package org.jdownloader.plugins.components.youtube.itag;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;
import org.jdownloader.translate._JDT;

public enum AudioCodec implements LabelInterface,TooltipInterface {
    AAC("Advanced Audio Codec", "AAC", 4, 10000),
    MP3("MP3", "MP3", 2, 10000),
    OPUS("Opus Audio", "Opus", 1, 10000),
    VORBIS("Vorbis Audio", "Vorbis", 3, 10000),
    AMR("Adaptive Multi-Rate Codec", "ARM", 1, 10000),
    VORBIS_SPATIAL(null, "Vorbis 4Ch", 0.3, 10000) {

        public String getLabelLong() {
            return _JDT.T.AudioCodec_vorbis_spatial();
        }

    },
    AAC_SPATIAL(null, "AAC 6Ch", 0.4, 10000) {

        public String getLabelLong() {
            return _JDT.T.AudioCodec_aac_spatial();
        }

    };
    private double rating = -1;
    private String label;
    private String labelLong;

    public String getLabelLong() {
        return labelLong;
    }

    private AudioCodec(String labelLong, String label, double rating, double modifier) {
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

    public String getLabel() {
        return label;
    }

    @Override
    public String getTooltip() {
        return getLabelLong();
    }

}
