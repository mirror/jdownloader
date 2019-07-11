package org.jdownloader.plugins.components.youtube;

import java.util.ArrayList;
import java.util.List;

import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.AbstractCustomValueGetter;
import org.appwork.storage.config.annotations.CustomStorageName;
import org.appwork.storage.config.annotations.CustomValueGetter;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DefaultJsonObject;
import org.appwork.storage.config.annotations.DefaultStringValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LabelInterface;
import org.appwork.storage.config.annotations.RequiresRestart;
import org.appwork.storage.config.annotations.StorageHandlerFactoryAnnotation;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.utils.StringUtils;
import org.appwork.utils.net.httpconnection.HTTPProxyStorable;
import org.jdownloader.plugins.components.youtube.configpanel.YoutubeVariantCollection;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.QualitySortIdentifier;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.keepForCompatibilitye.YoutubeCompatibility;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.config.PluginConfigInterface;
import org.jdownloader.plugins.config.PluginHost;
import org.jdownloader.plugins.config.Type;
import org.jdownloader.translate._JDT;

@CustomStorageName("youtube/Youtube")
@StorageHandlerFactoryAnnotation(YoutubeConfigStorageHandlerFactory.class)
@PluginHost(host = "youtube.com", type = Type.HOSTER)
public interface YoutubeConfig extends PluginConfigInterface {
    public abstract static class AbstractEnumOrderFixList extends AbstractCustomValueGetter<String[]> {
        private Class<? extends Enum> cls;

        public AbstractEnumOrderFixList(Class<? extends Enum> cls) {
            this.cls = cls;
        }

        @Override
        public String[] getValue(KeyHandler<String[]> keyHandler, String[] value) {
            List<? extends Enum> enums = YT_STATICS.defaultEnumList(cls, value);
            String[] ret = new String[enums.size()];
            for (int i = 0; i < ret.length; i++) {
                ret[i] = enums.get(i).name();
            }
            return ret;
        }
    }

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
        },
        ;
    }

    public static class NotNullCustomGetter extends AbstractCustomValueGetter<String> {
        @Override
        public String getValue(KeyHandler<String> keyHandler, String value) {
            if (StringUtils.isEmpty(value)) {
                value = keyHandler.getDefaultValue();
            }
            return value;
        }
    }

    public static class QualitySortIdentifierOrderAudioBitrate extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderAudioBitrate() {
            super(AudioBitrate.class);
        }
    }

    public static class QualitySortIdentifierOrderAudioCodec extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderAudioCodec() {
            super(AudioCodec.class);
        }
    }

    public static class QualitySortIdentifierOrderDefaultGetter extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderDefaultGetter() {
            super(QualitySortIdentifier.class);
        }
    }

    public static class QualitySortIdentifierOrderFiletype extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderFiletype() {
            super(FileContainer.class);
        }
    }

    public static class QualitySortIdentifierOrderResolution extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderResolution() {
            super(VideoResolution.class);
        }
    }

    public static class QualitySortIdentifierOrderVideoCodec extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderVideoCodec() {
            super(VideoCodec.class);
        }
    }

    public static class QualitySortIdentifierOrderVideoFramerate extends AbstractEnumOrderFixList {
        public QualitySortIdentifierOrderVideoFramerate() {
            super(VideoFrameRate.class);
        }
    }

    public static final double AVOID_DOUBLE_MOD = 100000d;
    static Object              NOTHING          = YoutubeCompatibility.moveJSonFiles("youtube/Youtube");

    @AboutConfig
    @DefaultStringValue(value = "*VIDEO_NAME* (*AUDIO_BITRATE*kbit_*AUDIO_CODEC*).*EXT*")
    @CustomValueGetter(NotNullCustomGetter.class)
    String getAudioFilenamePattern();

    @AboutConfig
    @DefaultJsonObject("[]")
    List<AudioBitrate> getBlacklistedAudioBitrates();

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
    @CustomValueGetter(NotNullCustomGetter.class)
    @DefaultStringValue("*VIDEO_NAME* (*QUALITY*).*EXT*")
    String getDescriptionFilenamePattern();

    @AboutConfig
    List<VariantIDStorable> getDisabledVariants();

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
    ArrayList<String> getExtraSubtitles();

    @AboutConfig
    @Deprecated
    String getFilenamePattern();

    @CustomValueGetter(NotNullCustomGetter.class)
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
    @CustomValueGetter(NotNullCustomGetter.class)
    @DefaultStringValue("*VIDEO_NAME*")
    String getPackagePattern();

    @AboutConfig
    String[] getPreferedSubtitleLanguages();

    @Deprecated
    /**
     * @deprecated use proxy and whitelist instead
     * @return
     */
    HTTPProxyStorable getProxy();

    @AboutConfig
    @CustomValueGetter(QualitySortIdentifierOrderDefaultGetter.class)
    String[] getQualitySortIdentifierOrder();

    @CustomValueGetter(QualitySortIdentifierOrderAudioBitrate.class)
    @AboutConfig
    String[] getQualitySortIdentifierOrderAudioBitrate();

    @AboutConfig
    @CustomValueGetter(QualitySortIdentifierOrderAudioCodec.class)
    String[] getQualitySortIdentifierOrderAudioCodec();

    // @AboutConfig
    // ArrayList<YoutubeCustomVariantStorable> getCustomVariants();
    //
    // void setCustomVariants(ArrayList<YoutubeCustomVariantStorable> list);
    @AboutConfig
    @CustomValueGetter(QualitySortIdentifierOrderFiletype.class)
    String[] getQualitySortIdentifierOrderFiletype();

    @AboutConfig
    @CustomValueGetter(QualitySortIdentifierOrderResolution.class)
    String[] getQualitySortIdentifierOrderResolution();

    @CustomValueGetter(QualitySortIdentifierOrderVideoCodec.class)
    @AboutConfig
    String[] getQualitySortIdentifierOrderVideoCodec();

    @CustomValueGetter(QualitySortIdentifierOrderVideoFramerate.class)
    @AboutConfig
    String[] getQualitySortIdentifierOrderVideoFramerate();

    @AboutConfig
    @CustomValueGetter(NotNullCustomGetter.class)
    @DefaultStringValue("*VIDEO_NAME* (*LNG[DISPLAY]*).*EXT*")
    String getSubtitleFilenamePattern();

    @AboutConfig
    ArrayList<String> getSubtitleWhiteList();

    @AboutConfig
    @DescriptionForConfigEntry("ID Pattern for dupe filtering. Tags: *CONTAINER**AUDIO_BITRATE**AUDIO_CODEC**DEMUX**SPATIAL*")
    @CustomValueGetter(NotNullCustomGetter.class)
    @DefaultStringValue("*CONTAINER* *AUDIO_BITRATE* *SPATIAL* kbit/s")
    @RequiresRestart("A JDownloader Restart is Required")
    String getVariantNamePatternAudio();

    @AboutConfig
    @DefaultStringValue("*3D* *360* *HEIGHT*p *FPS*fps *CONTAINER* - Video *AUDIO_CODEC* - Audio")
    @RequiresRestart("A JDownloader Restart is Required")
    @DescriptionForConfigEntry("ID Pattern for dupe filtering. Tags: *CONTAINER**HEIGHT**FPS**AUDIO_CODEC**3D**AUDIO_BITRATE**SPATIAL*")
    @CustomValueGetter(NotNullCustomGetter.class)
    String getVariantNamePatternVideo();

    @AboutConfig
    @CustomValueGetter(NotNullCustomGetter.class)
    @DefaultStringValue("*3D* *360* *VIDEO_NAME* (*H*p_*FPS*fps_*VIDEO_CODEC*-*AUDIO_BITRATE*kbit_*AUDIO_CODEC*).*EXT*")
    String getVideoFilenamePattern();

    @AboutConfig
    @DefaultBooleanValue(false)
    @RequiresRestart("A JDownloader Restart is Required")
    boolean isAdvancedVariantNamesEnabled();

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isAndroidSupportEnabled();

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isChooseAlternativeForMassChangeOrAddDialog();

    @DefaultBooleanValue(false)
    @AboutConfig
    boolean isCustomChunkValueEnabled();

    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Disable this if you do not want to use the new DASH Format. This will disable AUDIO only Downloads, and High Quality Video Downloads")
    @AboutConfig
    boolean isExternMultimediaToolUsageEnabled();

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

    void setChooseAlternativeForMassChangeOrAddDialog(boolean v);

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

    void setQualitySortIdentifierOrder(String[] s);

    void setQualitySortIdentifierOrderAudioBitrate(String[] s);

    void setQualitySortIdentifierOrderAudioCodec(String[] s);

    void setQualitySortIdentifierOrderFiletype(String[] s);

    void setQualitySortIdentifierOrderResolution(String[] s);

    void setQualitySortIdentifierOrderVideoCodec(String[] s);

    void setQualitySortIdentifierOrderVideoFramerate(String[] s);

    void setSetCustomUrlEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    void setSubtitleCopyforEachVideoVariant(boolean b);

    void setSubtitleFilenamePattern(String name);

    void setSubtitleWhiteList(ArrayList<String> list);

    void setVariantNamePatternAudio(String type);

    void setVariantNamePatternVideo(String type);

    void setVideoFilenamePattern(String name);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Preload 200kb and use FFProbe to detect the actual Audio Bitrate.")
    boolean isDoExtendedAudioBitrateLookupEnabled();

    void setDoExtendedAudioBitrateLookupEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(5)
    @DescriptionForConfigEntry("If a Variant is not available, JD will try up to * alternatives")
    int getAutoAlternativeSearchDepths();

    void setAutoAlternativeSearchDepths(int i);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("If disabled, JD will ignore the Youtube Collections, but create an extra link for every variant")
    boolean isCollectionMergingEnabled();

    void setCollectionMergingEnabled(boolean b);

    @RequiresRestart("A JDownloader Restart is Required")
    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isSegmentLoadingEnabled();

    void setSegmentLoadingEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isMetaDataEnabled();

    void setMetaDataEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isCrawlDupeCheckEnabled();

    void setCrawlDupeCheckEnabled(boolean b);
}