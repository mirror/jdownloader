package jd.controlling.reconnect;

/**
 * Exception gets thrown of External IP Check failed.
 * 
 * @author thomas
 * 
 */
public class GetIpException extends Exception {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public GetIpException(final Exception e) {
        super(e);
    }

    public GetIpException(final String string) {
        super(string);
    }

    public GetIpException(final String string, final Exception e) {
        super(string, e);
    }

}
