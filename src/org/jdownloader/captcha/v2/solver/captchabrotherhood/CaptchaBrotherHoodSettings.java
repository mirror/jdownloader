package org.jdownloader.captcha.v2.solver.captchabrotherhood;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface CaptchaBrotherHoodSettings extends ChallengeSolverConfig {
    @AboutConfig
    @DescriptionForConfigEntry("Your CaptchaBrotherHood Username")
    String getUser();

    void setUser(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Your CaptchaBrotherHood Password")
    String getPass();

    void setPass(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha WhiteList for hoster")
    String getWhiteList();

    void setWhiteList(String jser);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha BlackList for hoster")
    String getBlackList();

    void setBlackList(String jser);
}
