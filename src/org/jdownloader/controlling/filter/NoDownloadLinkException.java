package org.jdownloader.controlling.filter;

public class NoDownloadLinkException extends Exception {

    /**
     * 
     */
    public NoDownloadLinkException() {
        super();
    }

    /**
     * @param message
     * @param cause
     * @param enableSuppression
     * @param writableStackTrace
     */
    public NoDownloadLinkException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
        super(message, cause, enableSuppression, writableStackTrace);
    }

    /**
     * @param message
     * @param cause
     */
    public NoDownloadLinkException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public NoDownloadLinkException(String message) {
        super(message);
    }

    /**
     * @param cause
     */
    public NoDownloadLinkException(Throwable cause) {
        super(cause);
    }

}
