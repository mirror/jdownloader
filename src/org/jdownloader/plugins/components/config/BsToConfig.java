package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "bs.to", type = Type.CRAWLER)
public interface BsToConfig extends PluginConfigInterface {
    @AboutConfig
    // @DefaultStringValue("")
    @DescriptionForConfigEntry("Define priority of sources e.g. 'VOE, MIXdrop, Vidoza'. Only the first available source will be added. If none found, all will be added!")
    @Order(10)
    String getHosterPriorityString();

    void setHosterPriorityString(String str);
}