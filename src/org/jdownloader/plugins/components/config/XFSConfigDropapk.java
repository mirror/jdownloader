package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "drop.download", type = Type.HOSTER)
public interface XFSConfigDropapk extends XFSConfig {
    public static final XFSConfigDropapk.TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getWebsiteAllowMassLinkcheck_label() {
            return "Allow website mass linkchecking?\r\nIf enabled, filenames are sometimes invisible until download is started.";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Allow website mass linkchecking?\r\nIf enabled, filenames are sometimes invisible until download is started.")
    @Order(500)
    boolean isWebsiteAllowMassLinkcheck();

    void setWebsiteAllowMassLinkcheck(boolean b);
}