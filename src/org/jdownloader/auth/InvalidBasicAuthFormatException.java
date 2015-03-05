package org.jdownloader.auth;

public class InvalidBasicAuthFormatException extends Exception {

    public InvalidBasicAuthFormatException(String message) {
        super(message);
    }

    public InvalidBasicAuthFormatException(Throwable e) {
        super(e);
    }
}
