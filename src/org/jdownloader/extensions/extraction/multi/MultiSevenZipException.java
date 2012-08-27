package org.jdownloader.extensions.extraction.multi;

import net.sf.sevenzipjbinding.SevenZipException;

public class MultiSevenZipException extends SevenZipException {

    private final int code;

    public MultiSevenZipException(Throwable e, int exitCode) {
        super(e);
        this.code = exitCode;
    }

    /**
     * @return the code
     */
    public int getExitCode() {
        return code;
    }

}
