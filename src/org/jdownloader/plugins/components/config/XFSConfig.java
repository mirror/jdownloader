package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface XFSConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Prefer http instead of https (not recommended)?")
    @Order(30)
    boolean isPreferHTTP();

    void setPreferHTTP(boolean b);
}