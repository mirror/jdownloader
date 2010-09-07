package jd.controlling.reconnect.plugins;

public class ReconnectException extends Exception {

    public ReconnectException(final String string) {
        super(string);
    }

    public ReconnectException(final Throwable e) {
        super(e);
    }

}
