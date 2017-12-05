package org.jdownloader.captcha.v2.solver.browser;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface BrowserCaptchaSolverConfig extends ChallengeSolverConfig {
    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isAutoClickEnabled();

    void setAutoClickEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoOpenBrowserEnabled();

    void setAutoOpenBrowserEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Example: [ \"C:\\\\Program Files (x86)\\\\Google\\\\Chrome\\\\Application\\\\chrome.exe\", \"%s\" ]")
    String[] getBrowserCommandline();

    void setBrowserCommandline(String[] cmd);

    @DefaultIntValue(24613)
    @AboutConfig
    int getLocalHttpPort();

    void setLocalHttpPort(int port);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If enabled, JD will use your default browser to improve the captcha detection.")
    boolean isBrowserLoopEnabled();

    void setBrowserLoopEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("If enabled, JD will use the browserloop even if it is in silentmode")
    boolean isBrowserLoopDuringSilentModeEnabled();

    void setBrowserLoopDuringSilentModeEnabled(boolean b);

    @DefaultBooleanValue(false)
    boolean isBrowserLoopUserConfirmed();

    void setBrowserLoopUserConfirmed(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Easier Captchas: Get your current google.com SID and HSID Cookie value from your default browser and enter id here.")
    String getGoogleComCookieValueSID();

    @AboutConfig
    @DescriptionForConfigEntry("Easier Captchas: Get your current google.com SID and HSID Cookie value from your default browser and enter id here.")
    String getGoogleComCookieValueHSID();

    void setGoogleComCookieValueSID(String s);

    void setGoogleComCookieValueHSID(String s);
    // @AboutConfig
    // @DefaultBooleanValue(false)
    //
    // boolean isRecaptcha2Enabled();
    //
    // void setRecaptcha2Enabled(boolean b);
}
