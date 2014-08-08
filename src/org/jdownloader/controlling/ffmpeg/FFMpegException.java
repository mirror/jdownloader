package org.jdownloader.controlling.ffmpeg;

public class FFMpegException extends Exception {

    private String std;
    private String error;

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
