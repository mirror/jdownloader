package jd.controlling.reconnect.plugins.liveheader;

import jd.config.Configuration;
import jd.utils.JDUtilities;

import org.appwork.storage.config.defaults.DefaultFactory;

public class DefaultPassword extends DefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
    }

}
