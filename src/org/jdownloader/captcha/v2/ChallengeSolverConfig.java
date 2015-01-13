package org.jdownloader.captcha.v2;

import java.util.ArrayList;
import java.util.Map;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface ChallengeSolverConfig extends ConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isBlackWhiteListingEnabled();

    void setBlackWhiteListingEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isEnabled();

    void setEnabled(boolean b);

    @AboutConfig
    Map<String, Integer> getWaitForMap();

    void setWaitForMap(Map<String, Integer> map);

    @AboutConfig
    ArrayList<String> getBlacklistEntries();

    @AboutConfig
    ArrayList<String> getWhitelistEntries();

    void setBlacklistEntries(ArrayList<String> list);

    void setWhitelistEntries(ArrayList<String> list);

}
