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
    public static final YoutubeConfig                 CFG                                   = PluginJsonConfig.get(YoutubeConfig.class);
    public static final StorageHandler<YoutubeConfig> SH                                    = (StorageHandler<YoutubeConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_H263                     = SH.getKeyHandler("RatingCodecH263", IntegerKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_AUDIO_CODECS              = SH.getKeyHandler("BlacklistedAudioCodecs", ObjectKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_H264                     = SH.getKeyHandler("RatingCodecH264", IntegerKeyHandler.class);

    public static final StringKeyHandler              PACKAGE_PATTERN                       = SH.getKeyHandler("PackagePattern", StringKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_RESOLUTIONS               = SH.getKeyHandler("BlacklistedResolutions", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              LINKS                                 = SH.getKeyHandler("Links", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             ADVANCED_VARIANT_NAMES_ENABLED        = SH.getKeyHandler("AdvancedVariantNamesEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_VIDEO_FRAMERATES          = SH.getKeyHandler("BlacklistedVideoFramerates", ObjectKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_MP4                  = SH.getKeyHandler("RatingContainerMP4", IntegerKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_MP3                  = SH.getKeyHandler("RatingContainerMP3", IntegerKeyHandler.class);

    public static final IntegerKeyHandler             CHUNKS_COUNT                          = SH.getKeyHandler("ChunksCount", IntegerKeyHandler.class);

    public static final BooleanKeyHandler             CUSTOM_CHUNK_VALUE_ENABLED            = SH.getKeyHandler("CustomChunkValueEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_AUDIO_BITRATES            = SH.getKeyHandler("BlacklistedAudioBitrates", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_GROUPS                    = SH.getKeyHandler("BlacklistedGroups", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              PROXY                                 = SH.getKeyHandler("Proxy", ObjectKeyHandler.class);

    /**
     * sets the CUSTOM 'download from' field to: yourProtocolPreference + "://www.youtube.com/watch?v=" + videoID. Useful for when you don't
     * want courselist / playlist / variant information polluting URL.
     **/
    public static final BooleanKeyHandler             SET_CUSTOM_URL_ENABLED                = SH.getKeyHandler("SetCustomUrlEnabled", BooleanKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_AAC                  = SH.getKeyHandler("RatingContainerAAC", IntegerKeyHandler.class);

    public static final StringKeyHandler              SUBTITLE_FILENAME_PATTERN             = SH.getKeyHandler("SubtitleFilenamePattern", StringKeyHandler.class);

    public static final ObjectKeyHandler              SUBTITLE_WHITE_LIST                   = SH.getKeyHandler("SubtitleWhiteList", ObjectKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_M4A                  = SH.getKeyHandler("RatingContainerM4A", IntegerKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_VP8                      = SH.getKeyHandler("RatingCodecVP8", IntegerKeyHandler.class);

    public static final EnumKeyHandler                LINK_IS_VIDEO_AND_PLAYLIST_URL_ACTION = SH.getKeyHandler("LinkIsVideoAndPlaylistUrlAction", EnumKeyHandler.class);

    public static final StringKeyHandler              DESCRIPTION_FILENAME_PATTERN          = SH.getKeyHandler("DescriptionFilenamePattern", StringKeyHandler.class);

    public static final StringKeyHandler              FILENAME_PATTERN                      = SH.getKeyHandler("FilenamePattern", StringKeyHandler.class);

    public static final StringKeyHandler              AUDIO_FILENAME_PATTERN                = SH.getKeyHandler("AudioFilenamePattern", StringKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_VP9                      = SH.getKeyHandler("RatingCodecVP9", IntegerKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING60FPS                           = SH.getKeyHandler("Rating60Fps", IntegerKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_PROJECTIONS               = SH.getKeyHandler("BlacklistedProjections", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             PROXY_ENABLED                         = SH.getKeyHandler("ProxyEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler              IMAGE_FILENAME_PATTERN                = SH.getKeyHandler("ImageFilenamePattern", StringKeyHandler.class);

    /**
     * ID Pattern for dupe filtering. Tags: *CONTAINER**HEIGHT**FPS**AUDIO_CODEC**3D**AUDIO_BITRATE**SPATIAL*
     **/
    public static final StringKeyHandler              VARIANT_NAME_PATTERN_VIDEO            = SH.getKeyHandler("VariantNamePatternVideo", StringKeyHandler.class);

    public static final BooleanKeyHandler             FAST_LINK_CHECK_ENABLED               = SH.getKeyHandler("FastLinkCheckEnabled", BooleanKeyHandler.class);

    /**
     * Disable this if you do not want to use the new DASH Format. This will disable AUDIO only Downloads, and High Quality Video Downloads
     **/
    public static final BooleanKeyHandler             EXTERN_MULTIMEDIA_TOOL_USAGE_ENABLED  = SH.getKeyHandler("ExternMultimediaToolUsageEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             ANDROID_SUPPORT_ENABLED               = SH.getKeyHandler("AndroidSupportEnabled", BooleanKeyHandler.class);

    /**
     * ID Pattern for dupe filtering. Tags: *CONTAINER**AUDIO_BITRATE**AUDIO_CODEC**DEMUX**SPATIAL*
     **/
    public static final StringKeyHandler              VARIANT_NAME_PATTERN_AUDIO            = SH.getKeyHandler("VariantNamePatternAudio", StringKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_FILE_CONTAINERS           = SH.getKeyHandler("BlacklistedFileContainers", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED_VIDEO_CODECS              = SH.getKeyHandler("BlacklistedVideoCodecs", ObjectKeyHandler.class);

    public static final ObjectKeyHandler              DISABLED_VARIANTS                     = SH.getKeyHandler("DisabledVariants", ObjectKeyHandler.class);

    public static final EnumKeyHandler                LINK_IS_PLAYLIST_URL_ACTION           = SH.getKeyHandler("LinkIsPlaylistUrlAction", EnumKeyHandler.class);

    public static final StringListHandler             PREFERED_SUBTITLE_LANGUAGES           = SH.getKeyHandler("PreferedSubtitleLanguages", StringListHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_WEBM                 = SH.getKeyHandler("RatingContainerWEBM", IntegerKeyHandler.class);

    public static final StringKeyHandler              VIDEO_FILENAME_PATTERN                = SH.getKeyHandler("VideoFilenamePattern", StringKeyHandler.class);

    public static final ObjectKeyHandler              EXTRA_SUBTITLES                       = SH.getKeyHandler("ExtraSubtitles", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             SUBTITLE_COPYFOR_EACH_VIDEO_VARIANT   = SH.getKeyHandler("SubtitleCopyforEachVideoVariant", BooleanKeyHandler.class);
}