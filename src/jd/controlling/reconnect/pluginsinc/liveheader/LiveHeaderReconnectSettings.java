package jd.controlling.reconnect.pluginsinc.liveheader;

import jd.controlling.reconnect.pluginsinc.liveheader.remotecall.RouterData;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultFactory;
import org.appwork.storage.config.annotations.DefaultJsonObject;

public interface LiveHeaderReconnectSettings extends ConfigInterface {
    @AboutConfig
    @DefaultFactory(DefaultScript.class)
    String getScript();

    void setScript(String script);

    @AboutConfig
    @DefaultFactory(DefaultUsername.class)
    String getUserName();

    void setUserName(String str);

    @AboutConfig
    @DefaultFactory(DefaultPassword.class)
    String getPassword();

    void setPassword(String str);

    @AboutConfig
    @DefaultFactory(DefaultRouterIP.class)
    String getRouterIP();

    void setRouterIP(String str);

    @DefaultJsonObject("{}")
    void setRouterData(RouterData routerData);

    RouterData getRouterData();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoSearchBestMatchFilterEnabled();

    void setAutoSearchBestMatchFilterEnabled(boolean b);

}
