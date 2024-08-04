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

@PluginHost(host = "pornportal.com", type = Type.HOSTER)
public interface PornportalComConfig extends PluginConfigInterface {
    public static enum FilenameScheme implements LabelInterface {
        ORIGINAL {
            @Override
            public String getLabel() {
                return "Original";
            }
        },
        VIDEO_ID_TITLE_QUALITY_EXT {
            @Override
            public String getLabel() {
                return "Video-ID + Title + quality + file extension";
            }
        },
        TITLE_QUALITY_EXT {
            @Override
            public String getLabel() {
                return "Title + quality + file extension";
            }
        };
    }

    @AboutConfig
    @DefaultEnumValue("VIDEO_ID_TITLE_QUALITY_EXT")
    @DescriptionForConfigEntry("Select preferred filename scheme")
    @Order(50)
    FilenameScheme getFilenameScheme();

    void setFilenameScheme(FilenameScheme quality);

    public static enum QualitySelectionMode implements LabelInterface {
        ALL_SELECTED {
            @Override
            public String getLabel() {
                return "All selected qualities";
            }
        },
        BEST {
            @Override
            public String getLabel() {
                return "BEST";
            }
        };
    }

    public static enum StreamTypePreference implements LabelInterface {
        PROGRESSIVE_MP4 {
            @Override
            public String getLabel() {
                return "MP4 progressive";
            }
        },
        HLS {
            @Override
            public String getLabel() {
                return "MP4 HLS";
            }
        },
        ALL {
            @Override
            public String getLabel() {
                return "ALL";
            }
        }
    }

    @DefaultEnumValue("ALL_SELECTED")
    @DescriptionForConfigEntry("If preferred qualities are not found, all will be crawled instead")
    @Order(100)
    QualitySelectionMode getQualitySelectionMode();

    void setQualitySelectionMode(QualitySelectionMode quality);

    @AboutConfig
    @DefaultEnumValue("PROGRESSIVE_MP4")
    @DescriptionForConfigEntry("If preferred type is not found, all types will be crawled instead")
    @Order(110)
    StreamTypePreference getStreamTypePreference();

    void setStreamTypePreference(StreamTypePreference type);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("360p")
    @Order(200)
    boolean isSelectQuality360();

    void setSelectQuality360(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("480p")
    @Order(210)
    boolean isSelectQuality480();

    void setSelectQuality480(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("720p")
    @Order(220)
    boolean isSelectQuality720();

    void setSelectQuality720(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("1080p")
    @Order(230)
    boolean isSelectQuality1080();

    void setSelectQuality1080(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("4k")
    @Order(240)
    boolean isSelectQuality2160();

    void setSelectQuality2160(boolean b);
}