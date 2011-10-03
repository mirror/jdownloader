package jd.controlling.reconnect.plugins.liveheader;

import jd.controlling.reconnect.plugins.liveheader.remotecall.RouterData;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.appwork.storage.config.annotations.DefaultValue;

public interface LiveHeaderReconnectSettings extends ConfigInterface {
    @AboutConfig
    @DefaultValue(DefaultScript.class)
    String getScript();

    void setScript(String script);

    @AboutConfig
    @DefaultValue(DefaultUsername.class)
    String getUserName();

    void setUserName(String str);

    @AboutConfig
    @DefaultValue(DefaultPassword.class)
    String getPassword();

    void setPassword(String str);

    @AboutConfig
    @DefaultValue(DefaultRouterIP.class)
    String getRouterIP();

    void setRouterIP(String str);

    @DefaultObjectValue("{}")
    void setRouterData(RouterData routerData);

    RouterData getRouterData();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoSearchBestMatchFilterEnabled();

    void setAutoSearchBestMatchFilterEnabled(boolean b);

}
