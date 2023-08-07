package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "anonfiles.com", type = Type.HOSTER)
public interface AnonFilesComConfig extends PluginConfigInterface {
    final String                                       text_MaxSimultaneousFreeDownloads           = "Set max simultaneous free downloads";
    final String                                       text_AllowFallbackToLowerQuality            = "Allow fallback to lower quality video download if download of original quality fails?";
    final String                                       text_PreferredCdnNode                       = "Define preferred CDN node e.g. 'cdn-104.anonfiles.com'";
    final String                                       text_AllowDownloadOfFilesFlaggedAsDangerous = "Allow downloads of files flagged as 'dangerous'?";
    public static final AnonFilesComConfig.TRANSLATION TRANSLATION                                 = new TRANSLATION();

    public static class TRANSLATION {
        public String getMaxSimultaneousFreeDownloads_label() {
            return text_MaxSimultaneousFreeDownloads;
        }

        public String getAllowFallbackToLowerQuality_label() {
            return text_AllowFallbackToLowerQuality;
        }

        public String getPreferredCdnNode_label() {
            return text_PreferredCdnNode;
        }

        public String getAllowDownloadOfFilesFlaggedAsDangerous_label() {
            return text_AllowDownloadOfFilesFlaggedAsDangerous;
        }
    }

    @AboutConfig
    @DescriptionForConfigEntry(text_MaxSimultaneousFreeDownloads)
    @DefaultIntValue(2)
    @SpinnerValidator(min = 1, max = 20, step = 1)
    @Order(10)
    int getMaxSimultaneousFreeDownloads();

    void setMaxSimultaneousFreeDownloads(int maxFree);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_AllowFallbackToLowerQuality)
    @Order(20)
    boolean isAllowFallbackToLowerQuality();

    void setAllowFallbackToLowerQuality(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry(text_PreferredCdnNode)
    @Order(30)
    String getPreferredCdnNode();

    void setPreferredCdnNode(final String str);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_AllowDownloadOfFilesFlaggedAsDangerous)
    @Order(40)
    boolean isAllowDownloadOfFilesFlaggedAsDangerous();

    void setAllowDownloadOfFilesFlaggedAsDangerous(boolean b);
}