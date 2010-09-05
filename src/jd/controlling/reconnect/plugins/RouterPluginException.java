package jd.controlling.reconnect.plugins;

public class RouterPluginException extends Exception {

    public RouterPluginException(final Exception e) {
        super(e);
    }

    public RouterPluginException(final String string) {
        super(string);
    }

}
