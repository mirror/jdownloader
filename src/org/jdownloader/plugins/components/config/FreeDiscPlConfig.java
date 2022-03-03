package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "freedisc.pl", type = Type.HOSTER)
public interface FreeDiscPlConfig extends PluginConfigInterface {
    public static class TRANSLATION {
        public String isCrawlSubfolders_label() {
            return "Crawl subfolders?";
        }

        public String getStreamDownloadMode_label() {
            return "Select stream download mode:";
        }
    }

    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    @AboutConfig
    @DefaultBooleanValue(true)
    @Order(10)
    boolean isCrawlSubfolders();

    void setCrawlSubfolders(boolean b);

    public static enum StreamDownloadMode implements LabelInterface {
        NONE {
            @Override
            public String getLabel() {
                return "Do not download videostreams";
            }
        },
        PREFER_STREAM {
            @Override
            public String getLabel() {
                return "Prefer stream download over original file download";
            }
        },
        STREAM_AS_FALLBACK {
            @Override
            public String getLabel() {
                return "Download stream only if original download is not possible";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("STREAM_AS_FALLBACK")
    @DescriptionForConfigEntry("Select stream download mode:")
    @Order(20)
    StreamDownloadMode getStreamDownloadMode();

    void setStreamDownloadMode(final StreamDownloadMode quality);
}