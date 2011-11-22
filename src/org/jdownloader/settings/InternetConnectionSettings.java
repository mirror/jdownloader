package org.jdownloader.settings;

import java.util.ArrayList;

import jd.controlling.proxy.ProxyData;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.Description;

public interface InternetConnectionSettings extends ConfigInterface {

    @AboutConfig
    @Description("List of all external Proxies")
    void setCustomProxyList(ArrayList<ProxyData> ret);

    @DefaultJsonObject("[]")
    ArrayList<ProxyData> getCustomProxyList();

    @AboutConfig
    @Description("List of all available direct gateways. Invalid entries will be removed")
    void setDirectGatewayList(ArrayList<ProxyData> ret);

    @DefaultJsonObject("[]")
    ArrayList<ProxyData> getDirectGatewayList();

    @AboutConfig
    @Description("Is direct connection (no proxy) the default connection?")
    void setNoneDefault(boolean b);

    @DefaultBooleanValue(true)
    boolean isNoneDefault();

    @AboutConfig
    @Description("Use direct connection (no proxy) for proxy rotation?")
    void setNoneRotationEnabled(boolean proxyRotationEnabled);

    @DefaultBooleanValue(true)
    boolean isNoneRotationEnabled();
}
