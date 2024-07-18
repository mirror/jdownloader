package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.components.config.MegaNzConfig.InvalidOrMissingDecryptionKeyAction;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "mega.nz", type = Type.CRAWLER)
public interface MegaNzFolderConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getCrawlerSetFullPathAsPackagename_label() {
            return "Folder crawler: Set full path as package name (if disabled, only name of respective folder will be used as packagename)?";
        }

        public String getMaxCacheFolderDetails_label() {
            return "Folder crawler: Max. time(minutes) to cache folder details for faster crawling";
        }

        public String getInvalidOrMissingDecryptionKeyAction_label() {
            return "How to handle links with invalid or missing decryption keys?";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("CRAWLER_SET_FULL_PATH_AS_PACKAGENAME")
    @DescriptionForConfigEntry("Folder crawler: Set full path as package name (if disabled, only name of respective folder will be used as packagename)?")
    @Order(110)
    boolean isCrawlerSetFullPathAsPackagename();

    void setCrawlerSetFullPathAsPackagename(boolean b);

    @AboutConfig
    @SpinnerValidator(min = 0, max = 60, step = 1)
    @DefaultIntValue(5)
    @DescriptionForConfigEntry("Max. time(minutes) to cache folder details for faster crawling")
    @Order(120)
    int getMaxCacheFolderDetails();

    void setMaxCacheFolderDetails(int minutes);

    @AboutConfig
    @DefaultEnumValue("DEFAULT")
    @DefaultOnNull
    @DescriptionForConfigEntry("MEGA links by default contain a key which is needed to decrypt the file- and file/folder information. If you are adding a lot of links without key or invalid key, JDownloader can ask you to enter it which may be annoying for you. This setting allows you to customize how JDownloader should treat such links.")
    @Order(130)
    InvalidOrMissingDecryptionKeyAction getInvalidOrMissingDecryptionKeyAction();

    void setInvalidOrMissingDecryptionKeyAction(final InvalidOrMissingDecryptionKeyAction action);
}