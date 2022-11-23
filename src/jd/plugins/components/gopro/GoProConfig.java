package jd.plugins.components.gopro;

import java.util.Set;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "gopro.com", type = Type.HOSTER)
public interface GoProConfig extends PluginConfigInterface {
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isImproveCrawlerSpeedBySkippingSizeCheck();

    void setImproveCrawlerSpeedBySkippingSizeCheck(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isCrawlDownscaledVariants();

    void setCrawlDownscaledVariants(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isUseOriginalGoProFileNames();

    void setUseOriginalGoProFileNames(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isAddEachVariantAsExtraLink();

    void setAddEachVariantAsExtraLink(boolean b);

    @AboutConfig
    Set<GoProType> getTypesToCrawl();

    void setTypesToCrawl(Set<GoProType> list);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAddMediaTypeToFileName();

    void setAddMediaTypeToFileName(boolean type);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isLocalMediaCacheEnabled();

    void setLocalMediaCacheEnabled(boolean b);

    @DescriptionForConfigEntry("If enabled, all GoPro Links will get tagged to stay in the linkgrabber. Check the global 'Action On Moving Links To Downloadlist' property in the advanced config.")
    // TODO:@AboutConfig
    @DefaultBooleanValue(false)
    boolean isKeepLinksInLinkgrabber();

    void setKeepLinksInLinkgrabber(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("0 for everything. Set to>0 if you want to crawl only the latest media files")
    int getOnlyScanLastXDaysFromLibrary();

    void setOnlyScanLastXDaysFromLibrary(int b);
}