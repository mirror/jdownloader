package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "upstore.net", type = Type.HOSTER)
public interface UpstoReConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION                      = new TRANSLATION();
    final String                    text_ActivateReconnectWorkaround = "Activate reconnect workaround for free downloads? Prevents having to enter additional captchas in between downloads.";
    final String                    text_AllowMultipleFreeDownloads  = "Allow up to two free downloads instead of only one?";
    final String                    text_CustomUserAgentHeader       = "Set custom User-Agent header";

    public static class TRANSLATION {
        public String getActivateReconnectWorkaround_label() {
            return text_ActivateReconnectWorkaround;
        }

        public String getAllowMultipleFreeDownloads_label() {
            return text_AllowMultipleFreeDownloads;
        }

        public String getCustomUserAgentHeader_label() {
            return null;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("EXPERIMENTALHANDLING")
    @DescriptionForConfigEntry(text_ActivateReconnectWorkaround)
    @Order(10)
    boolean isActivateReconnectWorkaround();

    void setActivateReconnectWorkaround(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_ActivateReconnectWorkaround)
    @Order(20)
    boolean isAllowMultipleFreeDownloads();

    void setAllowMultipleFreeDownloads(boolean b);

    @AboutConfig
    @DefaultStringValue("Mozilla/5.0 (Windows NT 10.0; WOW64; rv:102.0) Gecko/20100101 Firefox/102.0")
    @DescriptionForConfigEntry(text_CustomUserAgentHeader)
    @Order(30)
    String getCustomUserAgentHeader();

    public void setCustomUserAgentHeader(final String str);
}