package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "m3u8", type = Type.CRAWLER)
public interface GenericM3u8DecrypterConfig extends PluginConfigInterface {
    public static final GenericM3u8DecrypterConfig.TRANSLATION TRANSLATION                       = new TRANSLATION();
    // final String text_CrawlMode = "Select crawl mode";
    final String                                               text_EnableFastLinkcheck          = "Enable fast linkcheck?";
    final String                                               text_AddBandwidthValueToFilenames = "Add bandwidth value to filenames?";

    public static class TRANSLATION {
        // public String getCrawlMode_label() {
        // return text_CrawlMode;
        // }
        public String getEnableFastLinkcheck_label() {
            return text_EnableFastLinkcheck;
        }

        public String getAddBandwidthValueToFilenames_label() {
            return text_AddBandwidthValueToFilenames;
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry(text_EnableFastLinkcheck)
    @Order(1)
    boolean isEnableFastLinkcheck();
    // TODO: Rename this and change it to ENUM so users can fine-tune how fast linkcheck should be e.g. with estimated filesize or without

    void setEnableFastLinkcheck(boolean b);
    // public static enum CrawlMode implements LabelInterface {
    // ALL {
    // @Override
    // public String getLabel() {
    // return "All qualities";
    // }
    // },
    // BEST {
    // @Override
    // public String getLabel() {
    // return "Only the best quality";
    // }
    // },
    // WORST {
    // @Override
    // public String getLabel() {
    // return "Only the worst quality";
    // }
    // },
    // SELECTED {
    // @Override
    // public String getLabel() {
    // return "Only selected qualities";
    // }
    // };
    // }
    //
    // @AboutConfig
    // @DefaultEnumValue("ALL")
    // @Order(10)
    // @DescriptionForConfigEntry(text_CrawlMode)
    // CrawlMode getCrawlMode();
    //
    // void setCrawlMode(final CrawlMode mode);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry(text_AddBandwidthValueToFilenames)
    @Order(20)
    boolean isAddBandwidthValueToFilenames();

    void setAddBandwidthValueToFilenames(boolean b);
}