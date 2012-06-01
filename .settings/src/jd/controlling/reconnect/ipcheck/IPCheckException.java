package jd.controlling.reconnect.ipcheck;

/**
 * General IP Check Exception. Ip Check failed de to various reasons. see
 * message
 * 
 * @author thomas
 * 
 */
public class IPCheckException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 4702245864932300075L;

    public IPCheckException(final Exception e) {
        super(e);
    }

    public IPCheckException(final String string) {
        super(string);
    }

}
