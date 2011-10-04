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
