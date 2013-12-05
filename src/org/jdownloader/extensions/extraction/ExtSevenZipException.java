package org.jdownloader.extensions.extraction;

import net.sf.sevenzipjbinding.SevenZipException;

public class ExtSevenZipException extends SevenZipException {

    private int exitCode;

    public int getExitCode() {
        return exitCode;
    }

    public ExtSevenZipException(int exitCodeCreateError, String string) {
        super(string);
        this.exitCode = exitCodeCreateError;

    }

}
