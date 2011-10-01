package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;
import org.jdownloader.settings.annotations.AboutConfig;

public interface PackagizerSettings extends ConfigInterface {
    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<PackagizerRule> getRuleList();

    void setRuleList(ArrayList<PackagizerRule> list);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPackagizerEnabled();

    void setPackagizerEnabled(boolean b);

}
