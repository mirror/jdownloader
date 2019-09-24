package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "dropbox.com", type = Type.HOSTER)
public interface DropBoxConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String isIncludeRootSubfolder_label() {
            return "Include root to subfolder structure";
        }

        public String isUseAPIBETA_label() {
            return "[BETA feature] Use API?";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    /** 2019-09-24: TODO: Remove this setting - first folder IS always root! */
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isIncludeRootSubfolder();

    void setIncludeRootSubfolder(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    @DescriptionForConfigEntry("If enabled, API will be used")
    boolean isUseAPIBETA();

    void setUseAPIBETA(boolean b);
}