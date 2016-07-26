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
        this(reason, null, null);
    }

    public SkipReasonException(SkipReason reason, Exception e) {
        this(reason, null, e);
    }

    public SkipReasonException(SkipReason reason, String message) {
        this(reason, message, null);
    }

    public SkipReasonException(SkipReason reason, String message, Exception e) {
        super(constructMessage(reason, message), e);
        this.reason = reason;
    }

    private static String constructMessage(SkipReason reason, String message) {
        if (message != null) {
            return reason.name() + ":" + message;
        } else {
            return reason.name();
        }
    }

}
