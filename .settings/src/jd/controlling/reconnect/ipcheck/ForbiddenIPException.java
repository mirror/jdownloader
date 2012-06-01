package jd.controlling.reconnect.ipcheck;

/**
 * the user can define a regex to filter undesired IPS. if this happens, this
 * exception gets thrown
 * 
 * @author thomas
 * 
 */
public class ForbiddenIPException extends IPCheckException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    public ForbiddenIPException(final String string) {
        super(string);
    }

}
