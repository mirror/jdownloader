package org.jdownloader.settings;

import java.util.ArrayList;

import jd.controlling.proxy.DirectGatewayData;
import jd.controlling.proxy.ProxyData;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.ValueValidator;

@ValueValidator(InternetConnectionSettingsValidator.class)
public interface InternetConnectionSettings extends ConfigInterface {

    @AboutConfig
    @Description("List of all external Proxies")
    void setCustomProxyList(ArrayList<ProxyData> ret);

    @DefaultObjectValue("[]")
    ArrayList<ProxyData> getCustomProxyList();

    @AboutConfig
    @Description("List of all available direct gateways. Invalid entries will be removed")
    void setDirectGatewayList(ArrayList<DirectGatewayData> ret);

    @DefaultObjectValue("[]")
    ArrayList<DirectGatewayData> getDirectGatewayList();

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
