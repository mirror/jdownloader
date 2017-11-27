package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "dropbox.com", type = Type.CRAWLER)
public interface DropBoxConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String isIncludeRootSubfolder_label() {
            return "Include root to subfolder structure";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isIncludeRootSubfolder();

    void setIncludeRootSubfolder(boolean b);
}