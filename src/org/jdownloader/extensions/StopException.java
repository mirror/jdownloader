package org.jdownloader.extensions;

public class StopException extends Exception {

    public StopException(String message) {
        super(message);
    }

    public StopException(Exception e) {
        super(e);
    }

}
