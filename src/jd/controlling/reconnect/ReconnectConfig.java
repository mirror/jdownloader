package jd.controlling.reconnect;

import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.PlainStorage;
import org.jdownloader.settings.annotations.AboutConfig;

@PlainStorage
public interface ReconnectConfig extends ConfigInterface {

    public static final String ACTIVE_PLUGIN_ID = "ActivePluginID";

    @AboutConfig
    String getActivePluginID();

    void setActivePluginID(String id);

    @DefaultIntValue(0)
    void setGlobalFailedCounter(int i);

    int getGlobalFailedCounter();

    @DefaultIntValue(0)
    void setFailedCounter(int i);

    int getFailedCounter();

    @DefaultIntValue(0)
    void setGlobalSuccessCounter(int i);

    int getGlobalSuccessCounter();

    @DefaultIntValue(0)
    void setSuccessCounter(int i);

    int getSuccessCounter();

    @AboutConfig
    @DefaultIntValue(30)
    int getSecondsToWaitForIPChange();

    void setSecondsToWaitForIPChange(int i);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isIPCheckGloballyDisabled();

    void setIPCheckGloballyDisabled(boolean b);

    @DefaultIntValue(5)
    @AboutConfig
    int getSecondsBeforeFirstIPCheck();

    void setSecondsBeforeFirstIPCheck(int seconds);

    @Description("Please enter Website for IPCheck here")
    @AboutConfig
    String getGlobalIPCheckUrl();

    void setGlobalIPCheckUrl(String url);

    @Description("Please enter Regex for IPCheck here")
    @AboutConfig
    @DefaultStringValue(IP.IP_PATTERN)
    String getGlobalIPCheckPattern();

    void setGlobalIPCheckPattern(String pattern);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isCustomIPCheckEnabled();

    void setCustomIPCheckEnabled(boolean b);

    @DefaultIntValue(5)
    @AboutConfig
    int getMaxReconnectRetryNum();

    void setMaxReconnectRetryNum(int num);

}
