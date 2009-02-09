package jd.controlling.reconnect;

import java.util.logging.Logger;

import jd.config.ConfigContainer;
import jd.utils.JDUtilities;

public abstract class ReconnectMethod {

    protected static Logger logger = JDUtilities.getLogger();

    protected transient ConfigContainer config;

    public ReconnectMethod() {
        config = null;
    }

    public abstract boolean doReconnect();

    public abstract void initConfig();

    public ConfigContainer getConfig() {
        if (config == null) {
            config = new ConfigContainer(this);
            initConfig();
        }
        return config;
    }

    public abstract void resetMethod();

}
