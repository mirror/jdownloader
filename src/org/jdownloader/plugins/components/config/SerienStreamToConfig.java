package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "s.to", type = Type.CRAWLER)
public interface SerienStreamToConfig extends PluginConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Define priority of languages e.g. 'Deutsch, Englisch' (= prefer DE over EN). Only the first available language will be added. If none of the preferred languages are found, all available will be added!")
    @Order(10)
    String getLanguagePriorityString();

    void setLanguagePriorityString(final String str);

    @AboutConfig
    @DescriptionForConfigEntry("Define priority of sources e.g. 'VOE, Streamtape, Vidoza'. Only the first available source will be added. If none of the preferred mirrors are found, all available will be added!")
    @Order(20)
    String getHosterPriorityString();

    void setHosterPriorityString(final String str);
}