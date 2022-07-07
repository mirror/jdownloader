package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "motherless.com", type = Type.HOSTER)
public interface MotherlessComConfig extends PluginConfigInterface {
    final String                    text_GroupCrawlerLimit = "Define limit for group crawler (0 = disable group crawler, -1 = unlimited)";
    public static final TRANSLATION TRANSLATION            = new TRANSLATION();

    public static class TRANSLATION {
        public String getPreferOriginal_label() {
            return text_GroupCrawlerLimit;
        }
    }

    @AboutConfig
    @SpinnerValidator(min = -1, max = 10000, step = 100)
    @DefaultIntValue(500)
    @DescriptionForConfigEntry(text_GroupCrawlerLimit)
    @Order(10)
    int getGroupCrawlerLimit();

    void setGroupCrawlerLimit(int num);
}