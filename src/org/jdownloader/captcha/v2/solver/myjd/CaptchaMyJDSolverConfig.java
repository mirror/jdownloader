package org.jdownloader.captcha.v2.solver.myjd;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface CaptchaMyJDSolverConfig extends ChallengeSolverConfig {

    @AboutConfig
    public String[] getWhitelist();

    public void setWhitelist(String[] list);

    @AboutConfig
    public String[] getBlacklist();

    public void setBlacklist(String[] list);

    public static enum BlackOrWhitelist {
        BLACKLIST,
        WHITELIST,
        NONE;
    }

    @AboutConfig
    @DefaultEnumValue("BLACKLIST")
    public BlackOrWhitelist getBlackOrWhitelistType();

    public void setBlackOrWhitelistType(BlackOrWhitelist list);
}
