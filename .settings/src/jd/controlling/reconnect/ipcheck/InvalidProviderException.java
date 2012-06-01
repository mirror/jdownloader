package jd.controlling.reconnect.ipcheck;

public class InvalidProviderException extends IPCheckException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public InvalidProviderException(final Exception e) {
        super(e);
    }

    public InvalidProviderException(final String string) {
        super(string);
    }

}
