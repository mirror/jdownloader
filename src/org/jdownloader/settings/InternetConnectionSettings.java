package org.jdownloader.settings;

import java.util.ArrayList;

import jd.controlling.proxy.ProxyData;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface InternetConnectionSettings extends ConfigInterface {

    @AboutConfig
    @DescriptionForConfigEntry("List of all external Proxies")
    void setCustomProxyList(ArrayList<ProxyData> ret);

    ArrayList<ProxyData> getCustomProxyList();

    @AboutConfig
    @DescriptionForConfigEntry("List of all available direct gateways. Invalid entries will be removed")
    void setDirectGatewayList(ArrayList<ProxyData> ret);

    @DefaultJsonObject("[]")
    ArrayList<ProxyData> getDirectGatewayList();

    @AboutConfig
    @DescriptionForConfigEntry("Is direct connection (no proxy) the default connection?")
    void setNoneDefault(boolean b);

    @DefaultBooleanValue(true)
    boolean isNoneDefault();

    @AboutConfig
    @DescriptionForConfigEntry("Use direct connection (no proxy) for proxy rotation?")
    void setNoneRotationEnabled(boolean proxyRotationEnabled);

    @DefaultBooleanValue(true)
    boolean isNoneRotationEnabled();

    @AboutConfig
    void setRouterIPCheckConnectTimeout(int timeout);

    @DefaultIntValue(2000)
    int getRouterIPCheckConnectTimeout();

    @AboutConfig
    void setRouterIPCheckReadTimeout(int timeout);

    @DefaultIntValue(5000)
    int getRouterIPCheckReadTimeout();
}
