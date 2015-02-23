package org.jdownloader.extensions.extraction;

public class ExtractionControllerException extends Exception {

    private final int exitCode;

    public int getExitCode() {
        return exitCode;
    }

    public ExtractionControllerException(int exitCodeCreateError, String string) {
        super(string);
        this.exitCode = exitCodeCreateError;
    }

    public ExtractionControllerException(int exitCodeCreateError) {
        this.exitCode = exitCodeCreateError;
    }
}
