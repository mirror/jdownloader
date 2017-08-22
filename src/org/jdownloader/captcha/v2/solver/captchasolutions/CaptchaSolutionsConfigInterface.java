package org.jdownloader.captcha.v2.solver.captchasolutions;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface CaptchaSolutionsConfigInterface extends ChallengeSolverConfig {
    @AboutConfig
    @DescriptionForConfigEntry("Your CaptchaSolutions.com API Secret")
    String getAPISecret();

    void setAPISecret(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Your CaptchaSolutions.com API Key")
    String getAPIKey();

    void setAPIKey(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Your CaptchaSolutions.com Username")
    String getUserName();

    void setUserName(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Your CaptchaSolutions.com Password")
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
    boolean isEnabled();
}
