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
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeVariantCollection;
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

    public static enum IfUrlisAPlaylistAction implements LabelInterface {
        ASK {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAPlaylistAction_ASK();
            }
        },
        NOTHING {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAPlaylistAction_NOTHING();
            }
        },
        PROCESS {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAPlaylistAction_PROCESS();
            }
        };
    }

    public static enum IfUrlisAVideoAndPlaylistAction implements LabelInterface {

        ASK {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_ASK();
            }
        },
        NOTHING {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_NOTHING();
            }
        },
        PLAYLIST_ONLY {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_PLAYLIST_ONLY();
            }
        },
        VIDEO_ONLY {
            @Override
            public String getLabel() {
                return _JDT.T.YoutubeDash_IfUrlisAVideoAndPlaylistAction_VIDEO_ONLY();
            }
        },;

    }

    static Object NOTHING = YoutubeCompatibility.moveJSonFiles("youtube/Youtube");

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*AUDIO_BITRATE*kbit_*AUDIO_CODEC*).*EXT*")
    String getAudioFilenamePattern();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<AudioBitrate> getBlacklistedAudioBitrates();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isChooseAlternativeForMassChangeOrAddDialog();

    void setChooseAlternativeForMassChangeOrAddDialog(boolean v);

    @AboutConfig

    List<AudioCodec> getBlacklistedAudioCodecs();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<FileContainer> getBlacklistedFileContainers();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VariantGroup> getBlacklistedGroups();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<Projection> getBlacklistedProjections();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VideoResolution> getBlacklistedResolutions();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VideoCodec> getBlacklistedVideoCodecs();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<VideoFrameRate> getBlacklistedVideoFramerates();

    @DefaultJsonObject("[]")
    List<AudioBitrate> getChooseVariantDialogBlacklistedAudioBitrates();

    @DefaultJsonObject("[]")
    List<AudioCodec> getChooseVariantDialogBlacklistedAudioCodecs();

    @DefaultJsonObject("[]")
    List<FileContainer> getChooseVariantDialogBlacklistedFileContainers();

    @DefaultJsonObject("[]")
    List<VariantGroup> getChooseVariantDialogBlacklistedGroups();

    @DefaultJsonObject("[]")
    List<Projection> getChooseVariantDialogBlacklistedProjections();

    @DefaultJsonObject("[]")
    List<VideoResolution> getChooseVariantDialogBlacklistedResolutions();

    @DefaultJsonObject("[]")
    List<VideoCodec> getChooseVariantDialogBlacklistedVideoCodecs();

    @DefaultJsonObject("[]")
    List<VideoFrameRate> getChooseVariantDialogBlacklistedVideoFramerates();

    @AboutConfig
    @DefaultIntValue(15)
    int getChunksCount();

    List<YoutubeVariantCollection> getCollections();

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*QUALITY*).*EXT*")
    String getDescriptionFilenamePattern();

    @AboutConfig
    List<VariantIDStorable> getDisabledVariants();

    @AboutConfig
    ArrayList<String> getExtraSubtitles();

    @AboutConfig
    @Deprecated
    String getFilenamePattern();

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*QUALITY*).*EXT*")
    String getImageFilenamePattern();

    @AboutConfig
    @DefaultEnumValue("ASK")
    YoutubeConfig.IfUrlisAPlaylistAction getLinkIsPlaylistUrlAction();

    @AboutConfig
    @DefaultEnumValue("ASK")
    YoutubeConfig.IfUrlisAVideoAndPlaylistAction getLinkIsVideoAndPlaylistUrlAction();

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME*")
    String getPackagePattern();

    @AboutConfig
    String[] getPreferedSubtitleLanguages();

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

    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    HTTPProxyStorable getProxy();

    @AboutConfig
    @DefaultIntValue(5)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRating60Fps();

    @AboutConfig
    @DefaultIntValue(25)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecH263();

    @AboutConfig
    @DefaultIntValue(40)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecH264();

    @AboutConfig
    @DefaultIntValue(20)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecVP8();

    @AboutConfig
    @DefaultIntValue(30)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingCodecVP9();

    @AboutConfig
    @DefaultIntValue(4)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerAAC();

    @AboutConfig
    @DefaultIntValue(5)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerM4A();

    @AboutConfig
    @DefaultIntValue(2)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerMP3();

    @AboutConfig
    @DefaultIntValue(60)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerMP4();

    @AboutConfig
    @DefaultIntValue(50)
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("Increase or decrease this value to modify the 'best video/audio/image available' - sorting")
    int getRatingContainerWEBM();

    @AboutConfig
    @DefaultStringValue("*VIDEO_NAME* (*LNG[DISPLAY]*).*EXT*")
    String getSubtitleFilenamePattern();

    @AboutConfig
    ArrayList<String> getSubtitleWhiteList();

    @AboutConfig
    @DescriptionForConfigEntry("ID Pattern for dupe filtering. Tags: *CONTAINER**AUDIO_BITRATE**AUDIO_CODEC**DEMUX**SPATIAL*")

    @DefaultStringValue("*CONTAINER* *AUDIO_BITRATE* *SPATIAL* kbit/s")
    @RequiresRestart("A JDownloader Restart is Required")
    String getVariantNamePatternAudio();

    @AboutConfig
    @DefaultStringValue("*3D* *360* *HEIGHT*p *FPS*fps *CONTAINER* - Video *AUDIO_CODEC* - Audio")
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("ID Pattern for dupe filtering. Tags: *CONTAINER**HEIGHT**FPS**AUDIO_CODEC**3D**AUDIO_BITRATE**SPATIAL*")

    String getVariantNamePatternVideo();

    @AboutConfig
    @DefaultStringValue("*3D* *360* *VIDEO_NAME* (*H*p_*FPS*fps_*VIDEO_CODEC*-*AUDIO_BITRATE*kbit_*AUDIO_CODEC*).*EXT*")
    String getVideoFilenamePattern();

    @AboutConfig
    @DefaultBooleanValue(false)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isAdvancedVariantNamesEnabled();

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isAndroidSupportEnabled();

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCustomChunkValueEnabled();

    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Disable this if you do not want to use the new DASH Format. This will disable AUDIO only Downloads, and High Quality Video Downloads")
    @AboutConfig
    boolean isExternMultimediaToolUsageEnabled();

    // @AboutConfig
    // ArrayList<YoutubeCustomVariantStorable> getCustomVariants();
    //
    // void setCustomVariants(ArrayList<YoutubeCustomVariantStorable> list);

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isFastLinkCheckEnabled();

    @DefaultBooleanValue(false)
    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    boolean isProxyEnabled();

    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("sets the CUSTOM 'download from' field to: http://www.youtube.com/watch?v=\" + videoID. Useful for when you don't want courselist / playlist / variant information polluting URL.")
    @AboutConfig
    boolean isSetCustomUrlEnabled();

    boolean isSubtitleCopyforEachVideoVariant();

    void setAdvancedVariantNamesEnabled(boolean b);

    void setAndroidSupportEnabled(boolean b);

    void setAudioFilenamePattern(String name);

    void setBlacklistedAudioBitrates(List<AudioBitrate> v);

    void setBlacklistedAudioCodecs(List<AudioCodec> v);

    void setBlacklistedFileContainers(List<FileContainer> v);

    void setBlacklistedGroups(List<VariantGroup> v);

    void setBlacklistedProjections(List<Projection> v);

    void setBlacklistedResolutions(List<VideoResolution> v);

    void setBlacklistedVideoCodecs(List<VideoCodec> v);

    void setBlacklistedVideoFramerates(List<VideoFrameRate> v);

    void setChooseVariantDialogBlacklistedAudioBitrates(List<AudioBitrate> v);

    void setChooseVariantDialogBlacklistedAudioCodecs(List<AudioCodec> v);

    void setChooseVariantDialogBlacklistedFileContainers(List<FileContainer> v);

    void setChooseVariantDialogBlacklistedGroups(List<VariantGroup> v);

    void setChooseVariantDialogBlacklistedProjections(List<Projection> v);

    void setChooseVariantDialogBlacklistedResolutions(List<VideoResolution> v);

    void setChooseVariantDialogBlacklistedVideoCodecs(List<VideoCodec> v);

    void setChooseVariantDialogBlacklistedVideoFramerates(List<VideoFrameRate> v);

    void setChunksCount(int count);

    @AboutConfig

    void setCollections(List<YoutubeVariantCollection> links);

    void setCustomChunkValueEnabled(boolean b);

    void setDescriptionFilenamePattern(String name);

    void setDisabledVariants(List<VariantIDStorable> variants);

    void setExternMultimediaToolUsageEnabled(boolean b);

    void setExtraSubtitles(ArrayList<String> list);

    void setFastLinkCheckEnabled(boolean b);

    @Deprecated
    void setFilenamePattern(String name);

    void setImageFilenamePattern(String name);

    void setLinkIsPlaylistUrlAction(YoutubeConfig.IfUrlisAPlaylistAction action);

    void setLinkIsVideoAndPlaylistUrlAction(YoutubeConfig.IfUrlisAVideoAndPlaylistAction action);

    void setPackagePattern(String pattern);

    void setPreferedSubtitleLanguages(String[] lngKeys);

    @Deprecated
    void setProxy(HTTPProxyStorable address);

    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    void setProxyEnabled(boolean b);

    void setRating60Fps(int rating);

    void setRatingCodecH263(int rating);

    void setRatingCodecH264(int rating);

    void setRatingCodecVP8(int rating);

    void setRatingCodecVP9(int rating);

    void setRatingContainerAAC(int rating);

    void setRatingContainerM4A(int rating);

    void setRatingContainerMP3(int rating);

    void setRatingContainerMP4(int rating);

    void setRatingContainerWEBM(int rating);

    void setSetCustomUrlEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    void setSubtitleCopyforEachVideoVariant(boolean b);

    void setSubtitleFilenamePattern(String name);

    void setSubtitleWhiteList(ArrayList<String> list);

    void setVariantNamePatternAudio(String type);

    void setVariantNamePatternVideo(String type);

    void setVideoFilenamePattern(String name);
}