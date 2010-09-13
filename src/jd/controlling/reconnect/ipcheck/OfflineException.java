package jd.controlling.reconnect.ipcheck;

/**
 * Thrown if the connection is offline. no external ip available, or 0.0.0.0 for
 * upnp routers
 * 
 * @author thomas
 * 
 */
public class OfflineException extends IPCheckException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public OfflineException(final String string) {
        super(string);
    }

}
