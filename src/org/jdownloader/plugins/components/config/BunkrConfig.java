package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "bunkr.la", type = Type.HOSTER)
public interface BunkrConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Bunkr filenames sometimes contain an internal fileID in the ending. If this setting is enabled, this will be removed.")
    @Order(10)
    boolean isFixFilename();

    void setFixFilename(boolean b);
}