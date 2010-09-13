package jd.controlling.reconnect.ipcheck;

/**
 * the external ip has an invalid ip range. for example IP ranges which are used
 * in Privat networks only
 * 
 * @author thomas
 * 
 */
public class InvalidIPRangeException extends IPCheckException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public InvalidIPRangeException(final String string) {
        super(string);
    }

}
