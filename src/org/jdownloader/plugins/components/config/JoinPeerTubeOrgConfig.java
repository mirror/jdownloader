package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "joinpeertube.org", type = Type.HOSTER)
public interface JoinPeerTubeOrgConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Prefer HLS instead of HTTP stream downloads if no official download option is available (disables resume and precise filesize display!)?")
    @Order(30)
    boolean isPreferHLS();

    void setPreferHLS(boolean b);
}