package jd.controlling.reconnect;

public class RouterPluginException extends Exception {

    private static final long serialVersionUID = 2065340827332607180L;

    public RouterPluginException(final Throwable e) {
        super(e);
    }

    public RouterPluginException(final String string) {
        super(string);
    }

}
