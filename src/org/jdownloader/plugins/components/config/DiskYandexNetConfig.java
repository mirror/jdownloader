package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultOnNull;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;

@PluginHost(host = "disk.yandex.net", type = Type.HOSTER)
public interface DiskYandexNetConfig extends PluginConfigInterface {
    public static final TRANSLATION TRANSLATION = new TRANSLATION();

    public static class TRANSLATION {
        public String getActionForQuotaLimitedFiles_label() {
            return "What to do with quota limited files in account download mode?";
        }

        public String getActionOnRateLimitReached_label() {
            return "What to do with files that JDownloader has moved/imported into your Yandex account?";
        }
    }

    public static enum ActionForQuotaLimitedFiles implements LabelInterface {
        WAIT_AND_RETRY_LATER {
            @Override
            public String getLabel() {
                return "Wait and retry later";
            }
        },
        MOVE_INTO_ACCOUNT {
            @Override
            public String getLabel() {
                return "Move/import into account and download";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("MOVE_INTO_ACCOUNT")
    @Order(100)
    @DescriptionForConfigEntry("What to do with quota limited files in account download mode?")
    @DefaultOnNull
    ActionForQuotaLimitedFiles getActionForQuotaLimitedFiles();

    void setActionForQuotaLimitedFiles(final ActionForQuotaLimitedFiles action);

    public static enum ActionForMovedFiles implements LabelInterface {
        DONT_TOUCH {
            @Override
            public String getLabel() {
                return "Don't touch";
            }
        },
        DELETE_FROM_ACCOUNT {
            @Override
            public String getLabel() {
                return "Delete from account";
            }
        },
        DELETE_FROM_ACCOUNT_AND_EMPTY_TRASH {
            @Override
            public String getLabel() {
                return "Delete from account & empty trash";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("DELETE_FROM_ACCOUNT")
    @Order(200)
    @DescriptionForConfigEntry("What to do with files that JDownloader has moved/imported into your Yandex account?")
    @DefaultOnNull
    ActionForMovedFiles getActionForMovedFiles();

    void setActionForMovedFiles(final ActionForMovedFiles action);
}