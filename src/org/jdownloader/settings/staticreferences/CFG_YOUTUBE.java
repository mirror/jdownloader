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
        ConfigUtils.printStaticMappings(YoutubeConfig.class);
    }

    // Static Mappings for interface org.jdownloader.plugins.components.youtube.YoutubeConfig
    public static final YoutubeConfig                 CFG                                       = PluginJsonConfig.get(YoutubeConfig.class);
    public static final StorageHandler<YoutubeConfig> SH                                        = (StorageHandler<YoutubeConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_H263                         = SH.getKeyHandler("RatingCodecH263", IntegerKeyHandler.class);

    public static final StringKeyHandler              PACKAGE_PATTERN                           = SH.getKeyHandler("PackagePattern", StringKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_H264                         = SH.getKeyHandler("RatingCodecH264", IntegerKeyHandler.class);

    public static final StringListHandler             BLACKLISTED_VARIANTS                      = SH.getKeyHandler("BlacklistedVariants", StringListHandler.class);

    public static final EnumKeyHandler                GROUP_LOGIC                               = SH.getKeyHandler("GroupLogic", EnumKeyHandler.class);

    public static final BooleanKeyHandler             SUBTITLES_ENABLED                         = SH.getKeyHandler("SubtitlesEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              EXTRA                                     = SH.getKeyHandler("Extra", ObjectKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_MP4                      = SH.getKeyHandler("RatingContainerMP4", IntegerKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_MP3                      = SH.getKeyHandler("RatingContainerMP3", IntegerKeyHandler.class);

    public static final IntegerKeyHandler             CHUNKS_COUNT                              = SH.getKeyHandler("ChunksCount", IntegerKeyHandler.class);

    public static final BooleanKeyHandler             CUSTOM_CHUNK_VALUE_ENABLED                = SH.getKeyHandler("CustomChunkValueEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler              BLACKLISTED                               = SH.getKeyHandler("Blacklisted", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             CREATE_BEST_AUDIO_VARIANT_LINK_ENABLED    = SH.getKeyHandler("CreateBestAudioVariantLinkEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler              DESCRIPTION_FILENAME_PATTERN              = SH.getKeyHandler("DescriptionFilenamePattern", StringKeyHandler.class);

    public static final ObjectKeyHandler              PROXY                                     = SH.getKeyHandler("Proxy", ObjectKeyHandler.class);

    /**
     * sets the CUSTOM 'download from' field to: yourProtocolPreference + "://www.youtube.com/watch?v=" + videoID. Useful for when you don't
     * want courselist / playlist / variant information polluting URL.
     **/
    public static final BooleanKeyHandler             SET_CUSTOM_URL_ENABLED                    = SH.getKeyHandler("SetCustomUrlEnabled", BooleanKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_AAC                      = SH.getKeyHandler("RatingContainerAAC", IntegerKeyHandler.class);

    public static final BooleanKeyHandler             CREATE_BEST_VIDEO_VARIANT_LINK_ENABLED    = SH.getKeyHandler("CreateBestVideoVariantLinkEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler              SUBTITLE_FILENAME_PATTERN                 = SH.getKeyHandler("SubtitleFilenamePattern", StringKeyHandler.class);

    public static final StringListHandler             EXTRA_VARIANTS                            = SH.getKeyHandler("ExtraVariants", StringListHandler.class);

    public static final ObjectKeyHandler              SUBTITLE_WHITE_LIST                       = SH.getKeyHandler("SubtitleWhiteList", ObjectKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_M4A                      = SH.getKeyHandler("RatingContainerM4A", IntegerKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_VP8                          = SH.getKeyHandler("RatingCodecVP8", IntegerKeyHandler.class);

    public static final ObjectKeyHandler              CUSTOM_VARIANTS                           = SH.getKeyHandler("CustomVariants", ObjectKeyHandler.class);

    public static final EnumKeyHandler                LINK_IS_VIDEO_AND_PLAYLIST_URL_ACTION     = SH.getKeyHandler("LinkIsVideoAndPlaylistUrlAction", EnumKeyHandler.class);

    public static final StringKeyHandler              AUDIO_FILENAME_PATTERN                    = SH.getKeyHandler("AudioFilenamePattern", StringKeyHandler.class);

    public static final StringKeyHandler              FILENAME_PATTERN                          = SH.getKeyHandler("FilenamePattern", StringKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CODEC_VP9                          = SH.getKeyHandler("RatingCodecVP9", IntegerKeyHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING60FPS                               = SH.getKeyHandler("Rating60Fps", IntegerKeyHandler.class);

    public static final BooleanKeyHandler             CREATE_BEST3DVARIANT_LINK_ENABLED         = SH.getKeyHandler("CreateBest3DVariantLinkEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             PROXY_ENABLED                             = SH.getKeyHandler("ProxyEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             DESCRIPTION_TEXT_ENABLED                  = SH.getKeyHandler("DescriptionTextEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler              IMAGE_FILENAME_PATTERN                    = SH.getKeyHandler("ImageFilenamePattern", StringKeyHandler.class);

    public static final BooleanKeyHandler             FAST_LINK_CHECK_ENABLED                   = SH.getKeyHandler("FastLinkCheckEnabled", BooleanKeyHandler.class);

    /**
     * If enabled, JD will not suggest 1400p and 2160p videos as 'Best' stream and download the 1080p stream instead.
     **/
    public static final BooleanKeyHandler             BEST_VIDEO_VARIANT1080P_LIMIT_ENABLED     = SH.getKeyHandler("BestVideoVariant1080pLimitEnabled", BooleanKeyHandler.class);

    /**
     * Disable this if you do not want to use the new DASH Format. This will disable AUDIO only Downloads, and High Quality Video Downloads
     **/
    public static final BooleanKeyHandler             EXTERN_MULTIMEDIA_TOOL_USAGE_ENABLED      = SH.getKeyHandler("ExternMultimediaToolUsageEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             ANDROID_SUPPORT_ENABLED                   = SH.getKeyHandler("AndroidSupportEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                LINK_IS_PLAYLIST_URL_ACTION               = SH.getKeyHandler("LinkIsPlaylistUrlAction", EnumKeyHandler.class);

    public static final StringKeyHandler              VIDEO3DFILENAME_PATTERN                   = SH.getKeyHandler("Video3DFilenamePattern", StringKeyHandler.class);

    public static final StringListHandler             PREFERED_SUBTITLE_LANGUAGES               = SH.getKeyHandler("PreferedSubtitleLanguages", StringListHandler.class);

    /**
     * Increase or decrease this value to modify the 'best video/audio/image available' - sorting
     **/
    public static final IntegerKeyHandler             RATING_CONTAINER_WEBM                     = SH.getKeyHandler("RatingContainerWEBM", IntegerKeyHandler.class);

    public static final BooleanKeyHandler             CREATE_BEST_SUBTITLE_VARIANT_LINK_ENABLED = SH.getKeyHandler("CreateBestSubtitleVariantLinkEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler              VIDEO_FILENAME_PATTERN                    = SH.getKeyHandler("VideoFilenamePattern", StringKeyHandler.class);

    public static final ObjectKeyHandler              EXTRA_SUBTITLES                           = SH.getKeyHandler("ExtraSubtitles", ObjectKeyHandler.class);

    public static final BooleanKeyHandler             SUBTITLE_COPYFOR_EACH_VIDEO_VARIANT       = SH.getKeyHandler("SubtitleCopyforEachVideoVariant", BooleanKeyHandler.class);

    public static final BooleanKeyHandler             CREATE_BEST_IMAGE_VARIANT_LINK_ENABLED    = SH.getKeyHandler("CreateBestImageVariantLinkEnabled", BooleanKeyHandler.class);

}