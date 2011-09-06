package jd.controlling.reconnect.plugins.liveheader;

import jd.config.Configuration;
import jd.controlling.reconnect.plugins.liveheader.remotecall.RouterData;
import jd.utils.JDUtilities;

import org.appwork.storage.config.StorageHandler;

public class LiveHeaderReconnectSettingsDefaults implements LiveHeaderReconnectSettings {

    public StorageHandler<?> getStorageHandler() {
        return null;
    }

    public String getScript() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_REQUESTS);
    }

    public void setScript(String script) {
    }

    public String getUserName() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_USER);
    }

    public void setUserName(String str) {
    }

    public String getPassword() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_PASS);
    }

    public void setPassword(String str) {
    }

    public String getRouterIP() {
        return JDUtilities.getConfiguration().getStringProperty(Configuration.PARAM_HTTPSEND_IP);
    }

    public void setRouterIP(String str) {
    }

    public void setRouterData(RouterData routerData) {
    }

    public RouterData getRouterData() {
        return new RouterData();
    }

    public boolean isAutoSearchBestMatchFilterEnabled() {
        return true;
    }

    public void setAutoSearchBestMatchFilterEnabled(boolean b) {
    }

}
