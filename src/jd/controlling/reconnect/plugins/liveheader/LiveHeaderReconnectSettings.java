package jd.controlling.reconnect.plugins.liveheader;

import jd.controlling.reconnect.plugins.liveheader.remotecall.RouterData;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.jdownloader.settings.annotations.AboutConfig;

@DefaultFactory(LiveHeaderReconnectSettingsDefaults.class)
public interface LiveHeaderReconnectSettings extends ConfigInterface {
    @AboutConfig
    String getScript();

    void setScript(String script);

    @AboutConfig
    String getUserName();

    void setUserName(String str);

    @AboutConfig
    String getPassword();

    void setPassword(String str);

    @AboutConfig
    String getRouterIP();

    void setRouterIP(String str);

    void setRouterData(RouterData routerData);

    RouterData getRouterData();

}
