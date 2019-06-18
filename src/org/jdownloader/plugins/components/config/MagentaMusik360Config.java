package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "magenta-musik-360.de", type = Type.CRAWLER)
public interface MagentaMusik360Config extends PluginConfigInterface {
    public static class TRANSLATION {
        public String isCrawlVR() {
            return "Crawl VR Videos?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isCrawlVR();

    void setCrawlVR(boolean b);
}