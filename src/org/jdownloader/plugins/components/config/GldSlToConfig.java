package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "goldesel.bz", type = Type.CRAWLER)
public interface GldSlToConfig extends PluginConfigInterface {
    @AboutConfig
    // @DefaultStringValue("")
    @DescriptionForConfigEntry("Define priority of sources e.g. 'mega.co.nz, filestore.to, ul.to'. Only the first available source will be added. If none of the preferred mirrors are found, all will be added!")
    @Order(10)
    String getHosterPriorityString();

    void setHosterPriorityString(String str);
}