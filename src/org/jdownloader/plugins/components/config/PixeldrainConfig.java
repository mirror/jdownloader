package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "pixeldrain.com", type = Type.HOSTER)
public interface PixeldrainConfig extends PluginConfigInterface {
    final String                                     text_ReconnectOnSpeedLimit = "Reconnect whenever speed limit is present (disable = allow to download with limited speeds)?";
    public static final PixeldrainConfig.TRANSLATION TRANSLATION                = new TRANSLATION();

    public static class TRANSLATION {
        public String getReconnectOnSpeedLimit_label() {
            return text_ReconnectOnSpeedLimit;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_ReconnectOnSpeedLimit)
    @Order(10)
    boolean isReconnectOnSpeedLimit();

    void setReconnectOnSpeedLimit(boolean b);
}