package jd.controlling.reconnect.pluginsinc.liveheader;

import jd.utils.JDUtilities;

import org.appwork.storage.config.defaults.AbstractDefaultFactory;

public class DefaultUsername extends AbstractDefaultFactory<String> {

    @Override
    public String getDefaultValue() {
        return JDUtilities.getConfiguration().getStringProperty("HTTPSEND_USER");
    }

}
