package org.jdownloader.plugins.components.realDebridCom;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

import jd.plugins.decrypter.Ardmediathek.Translation;

@PluginHost(host = "real-debrid.com", type = Type.HOSTER)
public interface RealDebridComConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getIgnoreServerSideChunksNum_label() {
            return "Ignore max chunks set by real-debrid.com?";
        }

        public String getUseSSLForDownload_label() {
            return "Use SSL/HTTPS for Downloads?";
        }
    }

    public static final Translation TRANSLATION = new Translation();

    @AboutConfig
    @DefaultBooleanValue(false)
    void setIgnoreServerSideChunksNum(boolean b);

    boolean isIgnoreServerSideChunksNum();

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isUseSSLForDownload();

    void setUseSSLForDownload(boolean b);
}