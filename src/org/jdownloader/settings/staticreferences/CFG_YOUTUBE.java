package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.storage.config.handler.StringListHandler;
import org.appwork.utils.Application;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.config.PluginJsonConfig;

public class CFG_YOUTUBE {
    public static void main(String[] args) {
        Application.setApplication(".jd_home");
        ConfigUtils.printStaticMappings(YoutubeConfig.class, null, "PluginJsonConfig.get");
    }

    // Static Mappings for interface org.jdownloader.plugins.components.youtube.YoutubeConfig
    public static final YoutubeConfig                 CFG                                                = PluginJsonConfig.get(YoutubeConfig.class);
    public static final StorageHandler<YoutubeConfig> SH                                                 = (StorageHandler<YoutubeConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER_AUDIO_BITRATE        = SH.getKeyHandler("QualitySortIdentifierOrderAudioBitrate", StringListHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_VIDEO_FRAMERATES                       = SH.getKeyHandler("BlacklistedVideoFramerates", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             CUSTOM_CHUNK_VALUE_ENABLED                         = SH.getKeyHandler("CustomChunkValueEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_AUDIO_BITRATES                         = SH.getKeyHandler("BlacklistedAudioBitrates", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_GROUPS                                 = SH.getKeyHandler("BlacklistedGroups", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_AUDIO_BITRATES   = SH.getKeyHandler("ChooseVariantDialogBlacklistedAudioBitrates", ObjectKeyHandler.class);

    /**
     * sets the CUSTOM 'download from' field to: http://www.youtube.com/watch?v=" + videoID. Useful for when you don't want courselist /
     * playlist / variant information polluting URL.
     **/
    public static final BooleanKeyHandler             SET_CUSTOM_URL_ENABLED                             = SH.getKeyHandler("SetCustomUrlEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             ADVANCED_VARIANT_NAMES_ENABLED                     = SH.getKeyHandler("AdvancedVariantNamesEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_VIDEO_CODECS     = SH.getKeyHandler("ChooseVariantDialogBlacklistedVideoCodecs", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              SUBTITLE_WHITE_LIST                                = SH.getKeyHandler("SubtitleWhiteList", ObjectKeyHandler.class);

    public static final StringKeyHandler              DESCRIPTION_FILENAME_PATTERN                       = SH.getKeyHandler("DescriptionFilenamePattern", StringKeyHandler.class);

    public static final EnumKeyHandler                LINK_IS_VIDEO_AND_PLAYLIST_URL_ACTION              = SH.getKeyHandler("LinkIsVideoAndPlaylistUrlAction", EnumKeyHandler.class);

    public static final StringKeyHandler              AUDIO_FILENAME_PATTERN                             = SH.getKeyHandler("AudioFilenamePattern", StringKeyHandler.class);

    public static final StringKeyHandler              FILENAME_PATTERN                                   = SH.getKeyHandler("FilenamePattern", StringKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_PROJECTIONS                            = SH.getKeyHandler("BlacklistedProjections", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             PROXY_ENABLED                                      = SH.getKeyHandler("ProxyEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_RESOLUTIONS      = SH.getKeyHandler("ChooseVariantDialogBlacklistedResolutions", ObjectKeyHandler.class);

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER_FILETYPE             = SH.getKeyHandler("QualitySortIdentifierOrderFiletype", StringListHandler.class);

    /**
     * ID Pattern for dupe filtering. Tags: *CONTAINER**HEIGHT**FPS**AUDIO_CODEC**3D**AUDIO_BITRATE**SPATIAL*
     **/
    public static final StringKeyHandler              VARIANT_NAME_PATTERN_VIDEO                         = SH.getKeyHandler("VariantNamePatternVideo", StringKeyHandler.class);

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER                      = SH.getKeyHandler("QualitySortIdentifierOrder", StringListHandler.class);

    public static final BooleanKeyHandler             ANDROID_SUPPORT_ENABLED                            = SH.getKeyHandler("AndroidSupportEnabled", BooleanKeyHandler.class);

    /**
     * Disable this if you do not want to use the new DASH Format. This will disable AUDIO only Downloads, and High Quality Video Downloads
     **/
    public static final BooleanKeyHandler             EXTERN_MULTIMEDIA_TOOL_USAGE_ENABLED               = SH.getKeyHandler("ExternMultimediaToolUsageEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_VIDEO_CODECS                           = SH.getKeyHandler("BlacklistedVideoCodecs", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              COLLECTIONS                                        = SH.getKeyHandler("Collections", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              DISABLED_VARIANTS                                  = SH.getKeyHandler("DisabledVariants", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_PROJECTIONS      = SH.getKeyHandler("ChooseVariantDialogBlacklistedProjections", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_RESOLUTIONS                            = SH.getKeyHandler("BlacklistedResolutions", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             FAST_LINK_CHECK_ENABLED                            = SH.getKeyHandler("FastLinkCheckEnabled", BooleanKeyHandler.class);

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER_RESOLUTION           = SH.getKeyHandler("QualitySortIdentifierOrderResolution", StringListHandler.class);

    public static final StringKeyHandler              IMAGE_FILENAME_PATTERN                             = SH.getKeyHandler("ImageFilenamePattern", StringKeyHandler.class);

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER_VIDEO_FRAMERATE      = SH.getKeyHandler("QualitySortIdentifierOrderVideoFramerate", StringListHandler.class);

    public static final IntegerKeyHandler             CHUNKS_COUNT                                       = SH.getKeyHandler("ChunksCount", IntegerKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_AUDIO_CODECS     = SH.getKeyHandler("ChooseVariantDialogBlacklistedAudioCodecs", ObjectKeyHandler.class);

    /**
     * ID Pattern for dupe filtering. Tags: *CONTAINER**AUDIO_BITRATE**AUDIO_CODEC**DEMUX**SPATIAL*
     **/
    public static final StringKeyHandler              VARIANT_NAME_PATTERN_AUDIO                         = SH.getKeyHandler("VariantNamePatternAudio", StringKeyHandler.class);

    public static final StringKeyHandler              VIDEO_FILENAME_PATTERN                             = SH.getKeyHandler("VideoFilenamePattern", StringKeyHandler.class);

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER_VIDEO_CODEC          = SH.getKeyHandler("QualitySortIdentifierOrderVideoCodec", StringListHandler.class);

    public static final ObjectKeyHandler              PROXY                                              = SH.getKeyHandler("Proxy", ObjectKeyHandler.class);

    public static final EnumKeyHandler                LINK_IS_PLAYLIST_URL_ACTION                        = SH.getKeyHandler("LinkIsPlaylistUrlAction", EnumKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_VIDEO_FRAMERATES = SH.getKeyHandler("ChooseVariantDialogBlacklistedVideoFramerates", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_AUDIO_CODECS                           = SH.getKeyHandler("BlacklistedAudioCodecs", ObjectKeyHandler.class);

    public static final StringKeyHandler              PACKAGE_PATTERN                                    = SH.getKeyHandler("PackagePattern", StringKeyHandler.class);

    public static final StringListHandler             QUALITY_SORT_IDENTIFIER_ORDER_AUDIO_CODEC          = SH.getKeyHandler("QualitySortIdentifierOrderAudioCodec", StringListHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_FILE_CONTAINERS  = SH.getKeyHandler("ChooseVariantDialogBlacklistedFileContainers", ObjectKeyHandler.class);

    public static final StringKeyHandler              SUBTITLE_FILENAME_PATTERN                          = SH.getKeyHandler("SubtitleFilenamePattern", StringKeyHandler.class);

    public static final BooleanKeyHandler             CHOOSE_ALTERNATIVE_FOR_MASS_CHANGE_OR_ADD_DIALOG   = SH.getKeyHandler("ChooseAlternativeForMassChangeOrAddDialog", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_FILE_CONTAINERS                        = SH.getKeyHandler("BlacklistedFileContainers", ObjectKeyHandler.class);

    public static final StringListHandler             PREFERED_SUBTITLE_LANGUAGES                        = SH.getKeyHandler("PreferedSubtitleLanguages", StringListHandler.class);

    public static final ObjectKeyHandler              EXTRA_SUBTITLES                                    = SH.getKeyHandler("ExtraSubtitles", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             SUBTITLE_COPYFOR_EACH_VIDEO_VARIANT                = SH.getKeyHandler("SubtitleCopyforEachVideoVariant", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              CHOOSE_VARIANT_DIALOG_BLACKLISTED_GROUPS           = SH.getKeyHandler("ChooseVariantDialogBlacklistedGroups", ObjectKeyHandler.class);
}