package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "maxstream.video", type = Type.HOSTER)
public interface XFSConfigVideoMaxstreamVideo extends XFSConfigVideo {
    // @AboutConfig
    // @DescriptionForConfigEntry("Define priority of preferred languages e.g. 'Latine, Svenska'")
    // @Order(200)
    // String getLanguagePriorityString();
    //
    // void setLanguagePriorityString(String str);
    final String                    text_AllowStreamDownloadAsFallbackOnCloudflareDuringOfficialDownloadAttempt = "Allow stream download as fallback on Cloudflare during official download attempt?";
    final String                    text_TryToSkipOfficialDownloadPageToAvoidCloudflare                         = "Official download: Try to skip download page to try to avoid Cloudflare?";
    public static final TRANSLATION TRANSLATION                                                                 = new TRANSLATION();

    public static class TRANSLATION {
        public String getAllowStreamDownloadAsFallbackOnCloudflareDuringOfficialDownloadAttempt_label() {
            return text_AllowStreamDownloadAsFallbackOnCloudflareDuringOfficialDownloadAttempt;
        }

        public String getTryToSkipOfficialDownloadPageToAvoidCloudflare_label() {
            return text_TryToSkipOfficialDownloadPageToAvoidCloudflare;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_AllowStreamDownloadAsFallbackOnCloudflareDuringOfficialDownloadAttempt)
    @Order(200)
    boolean isAllowStreamDownloadAsFallbackOnCloudflareDuringOfficialDownloadAttempt();

    void setAllowStreamDownloadAsFallbackOnCloudflareDuringOfficialDownloadAttempt(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_TryToSkipOfficialDownloadPageToAvoidCloudflare)
    @Order(201)
    boolean isTryToSkipOfficialDownloadPageToAvoidCloudflare();

    void setTryToSkipOfficialDownloadPageToAvoidCloudflare(boolean b);
}