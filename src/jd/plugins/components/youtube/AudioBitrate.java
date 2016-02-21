package jd.plugins.components.youtube;

public enum AudioBitrate implements MediaQualityInterface {
    KBIT_128(128, 1000),
    KBIT_152(152, 1000),
    KBIT_160(160, 1000),
    KBIT_192(192, 1000),
    KBIT_256(256, 1000),

    KBIT_32(32, 1000),
    KBIT_48(48, 1000),
    KBIT_64(64, 1000),
    KBIT_96(96, 1000),

    KBIT_32_ESTIMATED(31, 1000),
    KBIT_12(12, 1000),
    KBIT_24(24, 1000);
    private double rating = -1;

    private AudioBitrate(double rating, double modifier) {
        this.rating = rating / modifier;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

}
