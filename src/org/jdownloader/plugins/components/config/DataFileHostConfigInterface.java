package org.jdownloader.plugins.components.config;

import jd.plugins.hoster.DataFileHostCom.DataFileHostConfigInterface.TRANSLATION;

import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "datafilehost.com", type = Type.HOSTER)
public interface DataFileHostConfigInterface extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getFreeUnlimitedChunksEnabled_label() {
            return "Enable unlimited chunks for free mode [can cause issues]?";
        }
    }

    public static final DataFileHostConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

    @DefaultBooleanValue(false)
    @Order(8)
    boolean isFreeUnlimitedChunksEnabled();

    void setFreeUnlimitedChunksEnabled(boolean b);
}