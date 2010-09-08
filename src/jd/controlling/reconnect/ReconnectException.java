package jd.controlling.reconnect;

public class ReconnectException extends Exception {

    private static final long serialVersionUID = -1229064581052693709L;

    public ReconnectException(final String string) {
        super(string);
    }

    public ReconnectException(final Throwable e) {
        super(e);
    }

}
