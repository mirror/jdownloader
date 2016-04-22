package org.jdownloader.plugins.components.youtube.variants;

import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.TooltipInterface;

public enum FileContainer implements LabelInterface,TooltipInterface {
    JPG("jpg", "JPEG Image"),
    SRT("srt", "Subtitle"),
    THREEGP("3gp", "3GP Video", 4d),
    MP3("mp3", "MP3 Audio"),
    AAC("aac", "AAC Audio"),
    OGG("ogg", "OGG Audio"),

    MP4("mp4", "MP4 Video", 6d),
    // ma4 and aac have the same codec, but we prefer m4a
    M4A("m4a", "M4A Audio", 1 / 10000d),
    TXT("txt", "Text"),
    WEBM("webm", "Google WebM Video", 5d),
    FLV("flv", "Flash FLV Video", 1);
    private String extension     = null;
    private double qualityRating = 0;
    private String longLabel;

    public double getQualityRating() {
        return qualityRating;
    }

    private FileContainer(String fileExtension, String longLabel, double rating) {
        this.extension = fileExtension;
        this.qualityRating = rating;
        this.longLabel = longLabel;
    }

    private FileContainer(String fileExtension, String longLabel) {
        this.extension = fileExtension;
        this.longLabel = longLabel;
    }

    public String getLabel() {
        return extension;
    }

    public String getExtension() {
        return extension;
    }

    public void setQualityRating(double d) {
        this.qualityRating = d;
    }

    @Override
    public String getTooltip() {
        return longLabel;
    }

}
