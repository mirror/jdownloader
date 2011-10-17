package jd.controlling.reconnect.liveheader;

import jd.config.Configuration;
import jd.utils.JDUtilities;

import org.appwork.storage.config.defaults.AbstractDefaultFactory;

public class DefaultRouterIP extends AbstractDefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP);
    }

}
