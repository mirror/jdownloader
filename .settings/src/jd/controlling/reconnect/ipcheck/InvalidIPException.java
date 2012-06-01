package jd.controlling.reconnect.ipcheck;

/**
 * Means that the internet connection is offline, or that there is an invalid ip
 * due to another error
 * 
 * @author thomas
 * 
 */
public class InvalidIPException extends IPCheckException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public InvalidIPException(final String string) {
        super(string);
    }

}
