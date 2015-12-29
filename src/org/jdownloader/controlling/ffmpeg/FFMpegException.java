package org.jdownloader.controlling.ffmpeg;

public class FFMpegException extends Exception {

    private final String std;
    private final String error;

    public FFMpegException(final String string) {
        this(string, null, null);
    }

    public FFMpegException(String string, String std, String error) {
        super(string);
        this.std = std;
        this.error = error;
    }

    public String getStd() {
        return std;
    }

    public String getError() {
        return error;
    }

}
