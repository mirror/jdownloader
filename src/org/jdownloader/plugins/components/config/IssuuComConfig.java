package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "issuu.com", type = Type.HOSTER)
public interface IssuuComConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Prefer to download images even if PDF download is available?")
    @Order(30)
    boolean isPreferImagesOverPDF();

    void setPreferImagesOverPDF(boolean b);
}