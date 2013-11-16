package org.jdownloader.captcha.v2.solver.dbc;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface DeathByCaptchaSettings extends ConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Your deathbycaptcha.eu Username")
    String getUserName();

    void setUserName(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Your deathbycaptcha.eu Password")
    String getPassword();

    void setPassword(String jser);

    @AboutConfig
    @RequiresRestart("A JDownloader Restart is required after changes")
    @DefaultIntValue(5)
    @SpinnerValidator(min = 0, max = 25)
    @DescriptionForConfigEntry("Max. Captchas Parallel")
    int getThreadpoolSize();

    void setThreadpoolSize(int size);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Active the deathbycaptcha.eu service")
    boolean isEnabled();

    void setEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha WhiteList for hoster")
    String getWhiteList();

    void setWhiteList(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha BlackList for hoster")
    String getBlackList();

    void setBlackList(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Activate the Captcha Feedback")
    boolean isFeedBackSendingEnabled();

    void setFeedBackSendingEnabled(boolean b);

}
