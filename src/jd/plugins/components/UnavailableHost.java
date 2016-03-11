package jd.plugins.components;

/**
 * Multihoster (helper) class to map error messages/timeouts values. This allows you to throw within canHandle/handleMulti with the exact
 * same message!
 *
 * @author raztoki
 *
 */
public final class UnavailableHost {

    private String errorReason;
    private Long   errorTimeout;

    public UnavailableHost(final Long errorTimeout, final String errorReason) {
        this.errorTimeout = errorTimeout;
        this.errorReason = errorReason;
    }

    /**
     * @return the errorReason
     */
    public final String getErrorReason() {
        return errorReason;
    }

    /**
     * @param errorReason
     *            the errorReason to set
     */
    public final void setErrorReason(String errorReason) {
        this.errorReason = errorReason;
    }

    /**
     * @return the errorTimeout
     */
    public final Long getErrorTimeout() {
        return errorTimeout;
    }

    /**
     * @param errorTimeout
     *            the errorTimeout to set
     */
    public final void setErrorTimeout(Long errorTimeout) {
        this.errorTimeout = errorTimeout;
    }

}
