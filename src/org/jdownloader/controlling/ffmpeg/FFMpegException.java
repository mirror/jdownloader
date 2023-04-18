package org.jdownloader.controlling.ffmpeg;

public class FFMpegException extends Exception {
    public static enum ERROR {
        INCOMPATIBLE,
        TOO_OLD,
        PATH_LENGTH,
        UNKNOWN,
        DISK_FULL
    }

    private final String stdout;
    private final String stderr;
    private final ERROR  error;

    public ERROR getError() {
        return error;
    }

    public FFMpegException(final String string, Throwable cause) {
        super(string, cause);
        this.stderr = null;
        this.stdout = null;
        this.error = ERROR.UNKNOWN;
    }

    public FFMpegException(final String string) {
        this(string, null, null);
    }

    public FFMpegException(String string, String stdout, String stderr) {
        this(string, stdout, stderr, ERROR.UNKNOWN);
    }

    public FFMpegException(String string, String stdout, String stderr, ERROR error) {
        super(string);
        this.stdout = stdout;
        this.stderr = stderr;
        this.error = error != null ? error : ERROR.UNKNOWN;
    }

    public String getStdOut() {
        return stdout;
    }

    public String getStdErr() {
        return stderr;
    }
}
