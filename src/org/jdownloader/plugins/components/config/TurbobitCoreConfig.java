package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface TurbobitCoreConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable fast linkcheck [recommended]? If enabled, filesize won't get displayed until downloads are started.")
    boolean isEnableFastLinkcheck();

    void setEnableFastLinkcheck(boolean b);
}