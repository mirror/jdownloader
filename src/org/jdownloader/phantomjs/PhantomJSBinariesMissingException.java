package org.jdownloader.phantomjs;

public class PhantomJSBinariesMissingException extends Exception {

    public PhantomJSBinariesMissingException(String absolutePath) {
        super(absolutePath);
    }

}
