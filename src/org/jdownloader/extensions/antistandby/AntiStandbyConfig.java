package org.jdownloader.extensions.antistandby;

import jd.plugins.ExtensionConfigInterface;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;

public interface AntiStandbyConfig extends ExtensionConfigInterface {

    @AboutConfig
    @DefaultEnumValue("DOWNLOADING")
    public Mode getMode();

    public void setMode(Mode mode);
}
