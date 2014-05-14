package org.jdownloader.updatev2;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface InternetConnectionSettings extends ConfigInterface {

    public static final String PATH = "cfg/org.jdownloader.settings.InternetConnectionSettings";

    @AboutConfig
    @DescriptionForConfigEntry("List of all external Proxies")
    void setCustomProxyList(ArrayList<ProxyData> ret);

    ArrayList<ProxyData> getCustomProxyList();

    @AboutConfig
    @DescriptionForConfigEntry("Timeout for connecting to a httpserver")
    @SpinnerValidator(min = 0, max = 600000)
    @DefaultIntValue(10000)
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
    void setRouterIPCheckReadTimeout(int timeout);

    @DefaultIntValue(5000)
    int getRouterIPCheckReadTimeout();

    void setLatestProfile(String absolutePath);

    String getLatestProfile();

    @AboutConfig
    void setLocalPacScript(String script);

    String getLocalPacScript();

}
