package org.jdownloader.extensions.extraction.multi;

public class CheckException extends Exception {

    public CheckException(String string) {
        super(string);
    }

    public CheckException(String string, Throwable e) {
        super(string, e);
    }

}
