package org.jdownloader.plugins.components.youtube;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.CustomStorageName;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.StorageHandlerFactoryAnnotation;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.plugins.components.youtube.configpanel.Link;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.keepForCompatibilitye.YoutubeCompatibility;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.translate._JDT;

@CustomStorageName("youtube/Youtube")
@StorageHandlerFactoryAnnotation(YoutubeConfigStorageHandlerFactory.class)
public interface YoutubeConfig extends PluginConfigInterface {

    static Object NOTHING = YoutubeCompatibility.moveJSonFiles("youtube/Youtube");

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isAndroidSupportEnabled();

    void setAndroidSupportEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCustomChunkValueEnabled();

    void setCustomChunkValueEnabled(boolean b);

    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Disable this if you do not want to use the new DASH Format. This will disable AUDIO only Downloads, and High Quality Video Downloads")
    @AboutConfig
    boolean isExternMultimediaToolUsageEnabled();

    void setExternMultimediaToolUsageEnabled(boolean b);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isFastLinkCheckEnabled();

    void setFastLinkCheckEnabled(boolean b);

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("sets the CUSTOM 'download from' field to: yourProtocolPreference + \"://www.youtube.com/watch?v=\" + videoID. Useful for when you don't want courselist / playlist / variant information polluting URL.")
    @AboutConfig
    boolean isSetCustomUrlEnabled();

    void setSetCustomUrlEnabled(boolean b);

    @DefaultBooleanValue(false)
    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    boolean isProxyEnabled();

    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    void setProxyEnabled(boolean b);

    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    HTTPProxyStorable getProxy();

    @Deprecated
    void setProxy(HTTPProxyStorable address);

    public static enum IfUrlisAVideoAndPlaylistAction implements LabelInterface {

        ASK {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_ASK();
            }
        },
        VIDEO_ONLY {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_VIDEO_ONLY();
            }
        },
        PLAYLIST_ONLY {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_PLAYLIST_ONLY();
            }
        },
        NOTHING {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_NOTHING();
            }
        },;

    }

    public static enum IfUrlisAPlaylistAction implements LabelInterface {
        ASK {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAPlaylistAction_ASK();
            }
        },
        PROCESS {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAPlaylistAction_PROCESS();
            }
        },
        NOTHING {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAPlaylistAction_NOTHING();
            }
        };
    }

    @AboutConfig
    List<VariantIDStorable> getDisabledVariants();

    void setDisabledVariants(List<VariantIDStorable> variants);

    @AboutConfig
    @DefaultEnumValue("ASK")
    YoutubeConfig.IfUrlisAVideoAndPlaylistAction getLinkIsVideoAndPlaylistUrlAction();

    void setLinkIsVideoAndPlaylistUrlAction(YoutubeConfig.IfUrlisAVideoAndPlaylistAction action);

    @AboutConfig
    @DefaultEnumValue("ASK")
    YoutubeConfig.IfUrlisAPlaylistAction getLinkIsPlaylistUrlAction();

    void setLinkIsPlaylistUrlAction(YoutubeConfig.IfUrlisAPlaylistAction action);

    // @DefaultBooleanValue(false)
    // @AboutConfig
    // boolean isPreferHttpsEnabled();
    //
    // void setPreferHttpsEnabled(boolean b);

    // @DefaultBooleanValue(true)
    // @AboutConfig
    // boolean isBestGroupVariantEnabled();
    //
    // void setBestGroupVariantEnabled(boolean b);

    @AboutConfig
    @Deprecated
    String getFilenamePattern();

    @Deprecated
    void setFilenamePattern(String name);

    @AboutConfig
    @DefaultStringValue("*3D* *360* *VIDEO_NAME* (*H*p_*FPS*fps_*VIDEO_CODEC*-*AUDIO_BITRATE*kbit_*AUDIO_CODEC*).*EXT*")
    String getVideoFilenamePattern();

    void setVideoFilenamePattern(String name);

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*AUDIO_BITRATE*kbit_*AUDIO_CODEC*).*EXT*")
    String getAudioFilenamePattern();

    void setAudioFilenamePattern(String name);

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*LNG[DISPLAY]*).*EXT*")
    String getSubtitleFilenamePattern();

    void setSubtitleFilenamePattern(String name);

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*QUALITY*).*EXT*")
    String getDescriptionFilenamePattern();

    void setDescriptionFilenamePattern(String name);

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*QUALITY*).*EXT*")
    String getImageFilenamePattern();

    void setImageFilenamePattern(String name);

    @AboutConfig
    @DefaultIntValue(15)
    int getChunksCount();

    void setChunksCount(int count);

    @AboutConfig
    String[] getPreferedSubtitleLanguages();

    void setPreferedSubtitleLanguages(String[] lngKeys);

    @AboutConfig
    ArrayList<String> getSubtitleWhiteList();

    void setSubtitleWhiteList(ArrayList<String> list);

    @AboutConfig
    ArrayList<String> getExtraSubtitles();

    void setExtraSubtitles(ArrayList<String> list);

    // @AboutConfig
    // ArrayList<YoutubeCustomVariantStorable> getCustomVariants();
    //
    // void setCustomVariants(ArrayList<YoutubeCustomVariantStorable> list);

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME*")
    String getPackagePattern();

    void setPackagePattern(String pattern);

    @AboutConfig
    @DefaultBooleanValue(true)
    void setSubtitleCopyforEachVideoVariant(boolean b);

    boolean isSubtitleCopyforEachVideoVariant();

    @AboutConfig
    @DefaultIntValue(40)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecH264();

    void setRatingCodecH264(int rating);

    @AboutConfig
    @DefaultIntValue(25)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecH263();

    void setRatingCodecH263(int rating);

    @AboutConfig
    @DefaultIntValue(30)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecVP9();

    void setRatingCodecVP9(int rating);

    @AboutConfig
    @DefaultIntValue(20)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecVP8();

    void setRatingCodecVP8(int rating);

    @AboutConfig
    @DefaultIntValue(5)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRating60Fps();

    void setRating60Fps(int rating);

    @AboutConfig
    @DefaultIntValue(60)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerMP4();

    void setRatingContainerMP4(int rating);

    @AboutConfig
    @DefaultIntValue(50)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerWEBM();

    void setRatingContainerWEBM(int rating);

    @AboutConfig
    @DefaultIntValue(5)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerM4A();

    void setRatingContainerM4A(int rating);

    @AboutConfig
    @DefaultIntValue(4)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerAAC();

    void setRatingContainerAAC(int rating);

    @AboutConfig
    @DefaultIntValue(2)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerMP3();

    void setRatingContainerMP3(int rating);

    @AboutConfig
    @DefaultBooleanValue(false)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isAdvancedVariantNamesEnabled();

    void setAdvancedVariantNamesEnabled(boolean b);

    @AboutConfig
    @DefaultStringValue("*3D* *360* *HEIGHT*p *FPS*fps *CONTAINER* - Video *AUDIO_CODEC* - Audio")
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("ID Pattern for dupe filtering. Tags: *CONTAINER**HEIGHT**FPS**AUDIO_CODEC**3D**AUDIO_BITRATE**SPATIAL*")

    String getVariantNamePatternVideo();

    void setVariantNamePatternVideo(String type);

    @AboutConfig
    @DescriptionForConfigEntry("ID Pattern for dupe filtering. Tags: *CONTAINER**AUDIO_BITRATE**AUDIO_CODEC**DEMUX**SPATIAL*")

    @DefaultStringValue("*CONTAINER* *AUDIO_BITRATE* *SPATIAL* kbit/s")
    @RequiresRestart("A JDownloader Restart is Required")
    String getVariantNamePatternAudio();

    void setVariantNamePatternAudio(String type);

    @AboutConfig
    @DefaultJsonObject("[]")
    List<AudioBitrate> getBlacklistedAudioBitrates();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VariantGroup> getBlacklistedGroups();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<FileContainer> getBlacklistedFileContainers();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<Projection> getBlacklistedProjections();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VideoResolution> getBlacklistedResolutions();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VideoFrameRate> getBlacklistedVideoFramerates();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VideoCodec> getBlacklistedVideoCodecs();

    @AboutConfig

    List<AudioCodec> getBlacklistedAudioCodecs();

    void setBlacklistedAudioBitrates(List<AudioBitrate> v);

    void setBlacklistedGroups(List<VariantGroup> v);

    void setBlacklistedFileContainers(List<FileContainer> v);

    void setBlacklistedProjections(List<Projection> v);

    void setBlacklistedResolutions(List<VideoResolution> v);

    void setBlacklistedVideoFramerates(List<VideoFrameRate> v);

    void setBlacklistedVideoCodecs(List<VideoCodec> v);

    void setBlacklistedAudioCodecs(List<AudioCodec> v);

    @AboutConfig

    void setLinks(List<Link> links);

    List<Link> getLinks();
}