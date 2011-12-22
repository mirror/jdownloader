package org.jdownloader.extensions.cvidplay;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.PlainStorage;

@PlainStorage
public interface CVidPlayConfig extends ExtensionConfigInterface {
    // adds the enbtry to jd advanced config table

    @AboutConfig
    // default value
    @DefaultIntValue(4)
    // will be shown in the advanced config
    @Description("This is a bla value")
    int getBlaNum();

    void setBlaNum(int num);
}
