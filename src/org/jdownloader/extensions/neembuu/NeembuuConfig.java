package org.jdownloader.extensions.neembuu;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.Description;
import org.appwork.storage.config.annotations.PlainStorage;

@PlainStorage
public interface NeembuuConfig extends ExtensionConfigInterface {
    // adds the enbtry to jd advanced config table

    @AboutConfig
    @DefaultIntValue(4)
    // default value
    @Description("This is a bla value")
    // will be shown in the advanced config
    int getBlaNum();

    void setBlaNum(int num);
}
