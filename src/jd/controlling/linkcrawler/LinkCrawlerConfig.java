package jd.controlling.linkcrawler;

import java.util.List;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultStringArrayValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface LinkCrawlerConfig extends ConfigInterface {

    @DefaultIntValue(12)
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("max. number of linkcrawler threads")
    @SpinnerValidator(min = 1, max = 128)
    int getMaxThreads();

    void setMaxThreads(int i);

    @DefaultIntValue(20000)
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("max. time in ms before killing an idle linkcrawler thread")
    int getThreadKeepAlive();

    void setThreadKeepAlive(int i);

    @DefaultIntValue(2 * 1024 * 1024)
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("max. bytes for page request during deep decrypt")
    @SpinnerValidator(min = 1 * 1024 * 1024, max = 5 * 1024 * 1024)
    int getDeepDecryptLoadLimit();

    void setDeepDecryptLoadLimit(int l);

    @DefaultIntValue(2 * 1024 * 1024)
    @AboutConfig
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("max. file size in bytes during deep decrypt")
    @SpinnerValidator(min = -1, max = 50 * 1024 * 1024)
    int getDeepDecryptFileSizeLimit();

    void setDeepDecryptFileSizeLimit(int l);

    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("A Offline Link created to indicate to the users when unknown Exceptions are thrown or plugin returns null results.")
    boolean isAddDefectiveCrawlerTasksAsOfflineInLinkgrabber();

    void setAddDefectiveCrawlerTasksAsOfflineInLinkgrabber(boolean b);

    @DefaultBooleanValue(true)
    @AboutConfig
    boolean isLinkCrawlerRulesEnabled();

    @DefaultStringArrayValue({ "ADD_LINKS_DIALOG", "PASTE_LINKS_ACTION", "MYJD" })
    @AboutConfig
    String[] getAutoLearnExtensionOrigins();

    public void setAutoLearnExtensionOrigins(String[] origins);

    void setLinkCrawlerRulesEnabled(boolean b);

    @DefaultJsonObject("[]")
    @AboutConfig
    List<LinkCrawlerRuleStorable> getLinkCrawlerRules();

    void setLinkCrawlerRules(List<LinkCrawlerRuleStorable> linkCrawlerRules);

    public static enum DirectHTTPPermission {
        ALWAYS,
        RULES_ONLY,
        FORBIDDEN
    }

    @AboutConfig
    @DefaultEnumValue("ALWAYS")
    DirectHTTPPermission getDirectHTTPPermission();

    void setDirectHTTPPermission(DirectHTTPPermission e);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isAutoImportContainer();

    public void setAutoImportContainer(boolean b);

}
