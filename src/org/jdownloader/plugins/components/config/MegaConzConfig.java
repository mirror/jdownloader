package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.TakeValueFromSubconfig;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "mega.co.nz", type = Type.HOSTER)
public interface MegaConzConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION                                                     = new TRANSLATION();
    final String                    text_AllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore = "Automatically start from zero if file was downloaded partially via multihoster and is then tried to be resumed directly via MEGA?";

    public static class TRANSLATION {
        public String getCheckReserverTraffic_label() {
            return "Check reserved traffic?";
        }

        public String getUseSSL_label() {
            return "Use SSL?";
        }

        public String getUseTmpDecryptingFile_label() {
            return "Use tmp decrypting file?";
        }

        public String getHideApplication_label() {
            return "Use minimal set of http headers?";
        }

        public String getUseGlobalCDN_label() {
            return "Use global CDN?";
        }

        public String getAllowConcurrentDecryption_label() {
            return "Allow concurrent decryption?";
        }

        public String getAllowMultihostUsage_label() {
            return "Allow multihoster usage?";
        }

        public String getLimitMode_label() {
            return "Set preferred limit mode";
        }

        public String getMaxWaittimeOnLimitReachedMinutes_label() {
            return "Max. wait time minutes on limit reached";
        }

        public String getAllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore_label() {
            return text_AllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore;
        }

        public String getCrawlerSetFullPathAsPackagename_label() {
            return "Folder crawler: Set full path as package name (if disabled, only name of respective folder will be used as packagename)?";
        }

        public String getMaxCacheFolderDetails_label() {
            return "Folder crawler: Max. time(minutes) to cache folder details for faster crawling";
        }
    }

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("CHECK_RESERVED_V2")
    @DescriptionForConfigEntry("Check reserved traffic?")
    @Order(10)
    boolean isCheckReserverTraffic();

    void setCheckReserverTraffic(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("USE_SSL_V3")
    @DescriptionForConfigEntry("Use SSL?")
    @Order(20)
    boolean isUseSSL();

    void setUseSSL(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("USE_TMP_V2")
    @DescriptionForConfigEntry("Use tmp decrypting file?")
    @Order(30)
    boolean isUseTmpDecryptingFile();

    void setUseTmpDecryptingFile(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("HIDE_APP_V2")
    @DescriptionForConfigEntry("Use minimal set of http headers?")
    @Order(40)
    boolean isHideApplication();

    void setHideApplication(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("USE_GLOBAL_CDN")
    @DescriptionForConfigEntry("Use global CDN?")
    @Order(50)
    boolean isUseGlobalCDN();

    void setUseGlobalCDN(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ALLOW_CONCURRENT_DECRYPTION")
    @DescriptionForConfigEntry("Allow concurrent decryption?")
    @Order(60)
    boolean isAllowConcurrentDecryption();

    void setAllowConcurrentDecryption(boolean b);

    public static enum LimitMode implements LabelInterface {
        GLOBAL_RECONNECT {
            @Override
            public String getLabel() {
                return "Global: Wait or get new IP";
            }
        },
        GLOBAL_WAIT {
            @Override
            public String getLabel() {
                return "Global: wait";
            }
        },
        PER_FILE {
            @Override
            public String getLabel() {
                return "Per file: Wait";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("PER_FILE")
    @DescriptionForConfigEntry("Set preferred limit mode")
    @Order(70)
    LimitMode getLimitMode();

    void setLimitMode(final LimitMode limitMode);

    @AboutConfig
    @SpinnerValidator(min = 10, max = 360, step = 1)
    @DefaultIntValue(10)
    @TakeValueFromSubconfig("MAX_LIMIT_WAITTIME")
    @DescriptionForConfigEntry("Max. wait time minutes on limit reached")
    @Order(80)
    int getMaxWaittimeOnLimitReachedMinutes();

    void setMaxWaittimeOnLimitReachedMinutes(int minutes);

    @AboutConfig
    @DefaultBooleanValue(true)
    @TakeValueFromSubconfig("ALLOW_MULTIHOST_USAGE")
    @DescriptionForConfigEntry("Allow multihoster usage?")
    @Order(90)
    boolean isAllowMultihostUsage();

    void setAllowMultihostUsage(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @TakeValueFromSubconfig("ALLOW_START_FROM_ZERO_IF_DOWNLOAD_WAS_STARTED_VIA_MULTIHOSTER")
    @DescriptionForConfigEntry(text_AllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore)
    @Order(100)
    boolean isAllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore();

    void setAllowStartFromZeroIfDownloadWasStartedViaMultihosterBefore(boolean b);

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
}