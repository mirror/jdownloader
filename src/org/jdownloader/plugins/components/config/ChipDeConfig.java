package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "chip.de", type = Type.HOSTER)
public interface ChipDeConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Display externally hosted downloads as offline?")
    @Order(10)
    boolean isDisplayExternalDownloadsAsOffline();

    void setDisplayExternalDownloadsAsOffline(boolean b);
}