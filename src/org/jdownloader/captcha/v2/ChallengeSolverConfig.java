package org.jdownloader.captcha.v2;

import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface ChallengeSolverConfig extends ConfigInterface {

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isEnabled();

    void setEnabled(boolean b);

    @AboutConfig
    HashMap<String, Integer> getWaitForMap();

    void setWaitForMap(HashMap<String, Integer> map);

}
