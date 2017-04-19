package jd.plugins;

public class AccountInvalidException extends PluginException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AccountInvalidException() {
        super(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInvalidException(String message) {
        super(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE);
    }

    public AccountInvalidException(Throwable throwable, String message) {
        super(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_DISABLE, throwable);
    }
}
