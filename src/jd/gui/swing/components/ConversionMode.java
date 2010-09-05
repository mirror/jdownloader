package jd.gui.swing.components;

public enum ConversionMode {
    AUDIOMP3("Audio (MP3)", new String[] { ".mp3" }), VIDEOFLV("Video (FLV)", new String[] { ".flv" }), AUDIOMP3_AND_VIDEOFLV("Audio & Video (MP3 & FLV)", new String[] { ".mp3", ".flv" }), VIDEOMP4("Video (MP4)", new String[] { ".mp4" }), VIDEOWEBM("Video (Webm)", new String[] { ".webm" }), VIDEO3GP("Video (3GP)", new String[] { ".3gp" }), VIDEOPODCAST("Video (MP4-Podcast)", new String[] { ".mp4" }), VIDEOIPHONE("Video (iPhone)", new String[] { ".mp4" });

    private String   text;
    private String[] ext;

    ConversionMode(final String text, final String[] ext) {
        this.text = text;
        this.ext = ext;
    }

    public String getExtFirst() {
        return this.ext[0];
    }

    public String getText() {
        return this.text;
    }

    @Override
    public String toString() {
        return this.text;
    }

}