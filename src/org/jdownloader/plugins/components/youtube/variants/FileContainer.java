package org.jdownloader.plugins.components.youtube.variants;

public enum FileContainer {
    JPG("jpg"),
    SRT("srt"),
    THREEGP("3gp", 4d),
    MP3("mp3"),
    AAC("aac"),
    OGG("ogg"),

    MP4("mp4", 6d),
    // ma4 and aac have the same codec, but we prefer m4a
    M4A("m4a", 1 / 10000d),
    TXT("txt"),
    WEBM("webm", 5d),
    FLV("flv", 1);
    private String extension     = null;
    private double qualityRating = 0;

    public double getQualityRating() {
        return qualityRating;
    }

    private FileContainer(String fileExtension, double rating) {
        this.extension = fileExtension;
        this.qualityRating = rating;
    }

    private FileContainer(String fileExtension) {
        this.extension = fileExtension;
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

}
