package jd.plugins;

public class AccountUnavailableException extends PluginException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;
    private final long        timeout;

    public long getTimeout() {
        return timeout;
    }

    public AccountUnavailableException(final long timeout) {
        super(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        this.timeout = timeout;
    }

    public AccountUnavailableException(String message, final long timeout) {
        super(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
        this.timeout = timeout;
    }

    public AccountUnavailableException(Throwable throwable, String message, final long timeout) {
        super(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE, throwable);
        this.timeout = timeout;
    }
}
