package org.jdownloader.plugins;

public class SkipReasonException extends Exception {
    /**
     * 
     */
    private static final long serialVersionUID = 5834016260306143944L;
    private final SkipReason  reason;

    public SkipReason getSkipReason() {
        return reason;
    }

    public SkipReasonException(SkipReason reason) {
        this.reason = reason;
    }

}
