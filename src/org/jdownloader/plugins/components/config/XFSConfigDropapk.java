package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "drop.download", type = Type.HOSTER)
public interface XFSConfigDropapk extends XFSConfigVideo {
    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Allow website mass linkchecking? If enabled, filenames are sometimes invisible until download is started.")
    @Order(500)
    boolean isWebsiteAllowMassLinkcheck();

    void setWebsiteAllowMassLinkcheck(boolean b);
}