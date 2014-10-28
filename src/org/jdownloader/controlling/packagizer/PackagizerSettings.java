package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;

public interface PackagizerSettings extends ConfigInterface {

    @DefaultJsonObject("[]")
    @AboutConfig
    ArrayList<PackagizerRule> getRuleList();

    void setRuleList(ArrayList<PackagizerRule> list);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPackagizerEnabled();

    void setPackagizerEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isTryJD1ImportEnabled();

    void setTryJD1ImportEnabled(boolean b);

}
