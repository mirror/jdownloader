package org.jdownloader.captcha.v2;

import java.util.Map;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface ChallengeSolverConfig extends ConfigInterface {

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isEnabled();

    void setEnabled(boolean b);

    @AboutConfig
    Map<String, Integer> getWaitForMap();

    void setWaitForMap(Map<String, Integer> map);

}
