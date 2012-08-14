package org.jdownloader.extensions.streaming.dataprovider.rar;

public class PasswordNotFoundException extends Exception {

    public PasswordNotFoundException(String string) {
        super(string);
    }

    public PasswordNotFoundException(Throwable e) {
        super(e);
    }

}
