package org.jdownloader.updatev2;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.appwork.utils.net.httpconnection.HTTPConnectionUtils.IPVERSION;

public interface InternetConnectionSettings extends ConfigInterface {
    public static final String PATH = "cfg/org.jdownloader.settings.InternetConnectionSettings";

    @AboutConfig
    @DescriptionForConfigEntry("List of all external Proxies")
    void setCustomProxyList(ArrayList<ProxyData> ret);

    ArrayList<ProxyData> getCustomProxyList();

    @AboutConfig
    @DescriptionForConfigEntry("Set preferred IP version to use")
    @DefaultEnumValue("SYSTEM")
    @RequiresRestart("A JDownloader Restart is Required")
    IPVERSION getPreferredIPVersion();

    void setPreferredIPVersion(IPVERSION ipVersion);

    @AboutConfig
    @DescriptionForConfigEntry("Timeout for connecting to a httpserver")
    @SpinnerValidator(min = 0, max = 600000)
    @DefaultIntValue(20000)
    @RequiresRestart("A JDownloader Restart is Required")
    int getHttpConnectTimeout();

    @AboutConfig
    @DescriptionForConfigEntry("Timeout for reading from a httpserver")
    @SpinnerValidator(min = 0, max = 600000)
    @DefaultIntValue(60000)
    @RequiresRestart("A JDownloader Restart is Required")
    int getHttpReadTimeout();

    void setHttpConnectTimeout(int seconds);

    void setHttpReadTimeout(int seconds);

    @AboutConfig
    void setRouterIPCheckConnectTimeout(int timeout);

    @DefaultIntValue(2000)
    int getRouterIPCheckConnectTimeout();

    @AboutConfig
    void setRouterIPCheckReadTimeout2(int timeout);

    @DefaultIntValue(10000)
    int getRouterIPCheckReadTimeout2();

    void setLatestProfile(String absolutePath);

    String getLatestProfile();

    @AboutConfig
    void setLocalPacScript(String script);

    String getLocalPacScript();

    @AboutConfig
    @DefaultBooleanValue(true)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Proxy Vole is used to autodetect your proxy settings. If you know how to setup your proxy, you can disable this.")
    boolean isProxyVoleAutodetectionEnabled();

    void setProxyVoleAutodetectionEnabled(boolean b);
}
