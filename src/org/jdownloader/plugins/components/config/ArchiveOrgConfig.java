package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "archive.org", type = Type.CRAWLER)
public interface ArchiveOrgConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferOriginal_label() {
            return "Prefer original?";
        }

        public String getCrawlArchiveView_label() {
            return "Also crawl archive view?";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isPreferOriginal();

    void setPreferOriginal(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isCrawlArchiveView();

    void setCrawlArchiveView(boolean b);
}