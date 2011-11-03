package jd.controlling.linkcrawler;

public class OnlineStatusUncheckedException extends Exception {

    /**
     * 
     */
    public OnlineStatusUncheckedException() {
        super();
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public OnlineStatusUncheckedException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     * @param cause
     */
    public OnlineStatusUncheckedException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public OnlineStatusUncheckedException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public OnlineStatusUncheckedException(Throwable cause) {
        super(cause);
    }

}
