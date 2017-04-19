package jd.plugins;

public class AccountRequiredException extends PluginException {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public AccountRequiredException() {
        super(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    public AccountRequiredException(String message) {
        super(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_ONLY);
    }

    public AccountRequiredException(Throwable throwable, String message) {
        super(LinkStatus.ERROR_PREMIUM, message, PluginException.VALUE_ID_PREMIUM_ONLY, throwable);
    }
}
