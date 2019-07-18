package org.jdownloader.plugins.components.config;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.LabelInterface;
import org.jdownloader.plugins.config.Order;
import org.jdownloader.plugins.config.PluginConfigInterface;

public interface TvnowConfigInterface extends PluginConfigInterface {
    public static class TRANSLATION {
        public String getEnableUnlimitedSimultaneousDownloads_label() {
            /* Translation not required for this */
            return "Enable unlimited simultaneous downloads? [Warning this may cause issues]";
        }

        public String getEnableDRMOffline_label() {
            /* Translation not required for this */
            return "Display DRM protected content as offline (because it is not downloadable anyway)?";
        }

        public String getShowQualityInfoInComment() {
            /* Translation not required for this */
            return "Show quality information in comment field on downloadstart?";
        }
    }

    public static enum Quality implements LabelInterface {
        BEST {
            @Override
            public String getLabel() {
                return "Best";
            }
        },
        FHD1080 {
            @Override
            public String getLabel() {
                return "1080p";
            }
        },
        HD720 {
            @Override
            public String getLabel() {
                return "720p";
            }
        },
        SD540HIGH {
            @Override
            public String getLabel() {
                return "540p high";
            }
        },
        SD540LOW {
            @Override
            public String getLabel() {
                return "540p low";
            }
        },
        SD360HIGH {
            @Override
            public String getLabel() {
                return "360p high";
            }
        },
        SD360LOW {
            @Override
            public String getLabel() {
                return "360p low";
            }
        };
    }

    public static final TvnowConfigInterface.TRANSLATION TRANSLATION = new TRANSLATION();

    @DefaultBooleanValue(false)
    @Order(10)
    boolean isEnableUnlimitedSimultaneousDownloads();

    void setEnableUnlimitedSimultaneousDownloads(boolean b);

    @DefaultBooleanValue(false)
    @Order(20)
    boolean isEnableDRMOffline();

    void setEnableDRMOffline(boolean b);

    @DefaultBooleanValue(false)
    @Order(30)
    boolean isShowQualityInfoInComment();

    void setShowQualityInfoInComment(boolean b);

    @AboutConfig
    @DefaultEnumValue("BEST")
    @Order(40)
    TvnowConfigInterface.Quality getPreferredQuality();

    void setPreferredQuality(TvnowConfigInterface.Quality quality);
}