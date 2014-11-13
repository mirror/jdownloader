package org.jdownloader.captcha.v2.solver.jac;

import java.util.HashMap;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.jdownloader.captcha.v2.ChallengeSolverConfig;

public interface JACSolverConfig extends ChallengeSolverConfig {

    @AboutConfig
    HashMap<String, AutoTrust> getJACThreshold();

    void setJACThreshold(HashMap<String, AutoTrust> map);

    @AboutConfig
    @DefaultIntValue(90)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Do not Change me unless you know 100000% what this value is used for!")
    int getDefaultJACTrustThreshold();

    void setDefaultJACTrustThreshold(int value);

}
