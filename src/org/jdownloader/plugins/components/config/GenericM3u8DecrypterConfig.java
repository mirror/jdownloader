package org.jdownloader.plugins.components.config;

import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "m3u8", type = Type.CRAWLER)
public interface GenericM3u8DecrypterConfig extends PluginConfigInterface {
    /* 2022-08-30: TODO: So far this is only a mockup and isn't being used. */
    // public static final GenericM3u8DecrypterConfig.TRANSLATION TRANSLATION = new TRANSLATION();
    // final String text_CrawlMode = "Select crawl mode";
    //
    // public static class TRANSLATION {
    // public String getCrawlMode_label() {
    // return text_CrawlMode;
    // }
    // }
    //
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
    // @Order(40)
    // @DescriptionForConfigEntry(text_CrawlMode)
    // CrawlMode getCrawlMode();
    //
    // void setCrawlMode(final CrawlMode mode);
}