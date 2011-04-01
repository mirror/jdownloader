package org.jdownloader.extensions;

public class StopException extends Exception {

    private static final long serialVersionUID = 1432912973477682193L;

    public StopException(String message) {
        super(message);
    }

    public StopException(Exception e) {
        super(e);
    }

}
