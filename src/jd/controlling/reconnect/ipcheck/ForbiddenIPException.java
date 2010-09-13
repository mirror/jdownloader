package jd.controlling.reconnect.ipcheck;

public class ForbiddenIPException extends IPCheckException {

    public ForbiddenIPException(final String string) {
        super(string);
    }

}
