package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "archive.org", type = Type.CRAWLER)
public interface ArchiveOrgConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String isPreferOriginal_label() {
            return "Prefer original?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isPreferOriginal();

    void setPreferOriginal(boolean b);
}