package org.jdownloader.extensions;

public class StartException extends Exception {

    private static final long serialVersionUID = 1L;

    public StartException(String msg) {
        super(msg);
    }

    public StartException(Throwable e) {
        super(e);
    }

}
