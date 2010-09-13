package jd.controlling.reconnect.plugins.upnp;

import jd.controlling.reconnect.ipcheck.IPCheckException;

public class InvalidProviderException extends IPCheckException {

    public InvalidProviderException(final Exception e) {
        super(e);
    }

    public InvalidProviderException(final String string) {
        super(string);
    }

}
