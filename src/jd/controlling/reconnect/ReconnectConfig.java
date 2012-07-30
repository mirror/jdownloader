package jd.controlling.reconnect;

import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.PlainStorage;
import org.appwork.storage.config.annotations.SpinnerValidator;

@PlainStorage
public interface ReconnectConfig extends ConfigInterface {

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
    @DefaultIntValue(300)
    @SpinnerValidator(min = 0, max = 600)
    int getSecondsToWaitForIPChange();

    void setSecondsToWaitForIPChange(int i);

    @AboutConfig
    @DefaultIntValue(60)
    @SpinnerValidator(min = 0, max = 300)
    int getSecondsToWaitForOffline();

    void setSecondsToWaitForOffline(int i);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isIPCheckGloballyDisabled();

    void setIPCheckGloballyDisabled(boolean b);

    @DefaultIntValue(5)
    @AboutConfig
    @SpinnerValidator(min = 0, max = 300)
    int getSecondsBeforeFirstIPCheck();

    void setSecondsBeforeFirstIPCheck(int seconds);

    @DescriptionForConfigEntry("Please enter Website for IPCheck here")
    @AboutConfig
    String getGlobalIPCheckUrl();

    void setGlobalIPCheckUrl(String url);

    @DescriptionForConfigEntry("Please enter Regex for IPCheck here")
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

    @AboutConfig
    @DefaultIntValue(2000)
    int getIPCheckConnectTimeout();

    void setIPCheckConnectTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(10000)
    int getIPCheckReadTimeout();

    void setIPCheckReadTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(5)
    @DescriptionForConfigEntry("Auto Reconnect Wizard performs a few reconnects for each successful script to find the fastest one. The more rounds we use, the better the result will be, but the longer it will take.")
    @SpinnerValidator(min = 1, max = 20)
    int getOptimizationRounds();

    void setOptimizationRounds(int num);

}
