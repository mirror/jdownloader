package org.jdownloader.controlling.packagizer;

import java.util.ArrayList;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.StorageHandler;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultObjectValue;

public interface PackagizerSettings extends ConfigInterface {

    public static final PackagizerSettings                 CFG = JsonConfig.create(PackagizerSettings.class);
    public static final StorageHandler<PackagizerSettings> SH  = (StorageHandler<PackagizerSettings>) CFG.getStorageHandler();

    @DefaultObjectValue("[]")
    @AboutConfig
    ArrayList<PackagizerRule> getRuleList();

    void setRuleList(ArrayList<PackagizerRule> list);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isPackagizerEnabled();

    void setPackagizerEnabled(boolean b);

}
