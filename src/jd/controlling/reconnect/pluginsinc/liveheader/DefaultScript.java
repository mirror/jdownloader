package jd.controlling.reconnect.pluginsinc.liveheader;

import jd.config.Configuration;
import jd.utils.JDUtilities;

import org.appwork.storage.config.defaults.AbstractDefaultFactory;

public class DefaultScript extends AbstractDefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
    }

}
