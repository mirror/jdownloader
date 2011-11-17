package jd.controlling.reconnect.liveheader;

import jd.utils.JDUtilities;

import org.appwork.storage.config.defaults.AbstractDefaultFactory;

public class DefaultRouterIP extends AbstractDefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        return JDUtilities.getConfiguration().getStringProperty("HTTPSEND_IP");
    }

}
