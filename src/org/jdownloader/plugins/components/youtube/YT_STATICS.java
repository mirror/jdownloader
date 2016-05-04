package org.jdownloader.plugins.components.youtube;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.appwork.storage.config.ValidationException;
import org.appwork.storage.config.events.GenericConfigEventListener;
import org.appwork.storage.config.handler.KeyHandler;
import org.appwork.txtresource.TranslationFactory;
import org.jdownloader.plugins.components.youtube.itag.AudioBitrate;
import org.jdownloader.plugins.components.youtube.itag.AudioCodec;
import org.jdownloader.plugins.components.youtube.itag.QualitySortIdentifier;
import org.jdownloader.plugins.components.youtube.itag.VideoCodec;
import org.jdownloader.plugins.components.youtube.itag.VideoFrameRate;
import org.jdownloader.plugins.components.youtube.itag.VideoResolution;
import org.jdownloader.plugins.components.youtube.variants.FileContainer;
import org.jdownloader.plugins.config.PluginJsonConfig;
import org.jdownloader.settings.staticreferences.CFG_YOUTUBE;

public class YT_STATICS {

    public static final YoutubeConfig           CFG = PluginJsonConfig.get(YoutubeConfig.class);

    public static Map<VideoResolution, Integer> SORTIDS_VIDEO_RESOLUTION;
    public static Map<VideoCodec, Integer>      SORTIDS_VIDEO_CODEC;
    public static Map<VideoFrameRate, Integer>  SORTIDS_VIDEO_FRAMERATE;
    public static Map<AudioBitrate, Integer>    SORTIDS_AUDIO_BITRATE;
    public static List<QualitySortIdentifier>   SORTIDS;
    public static Map<AudioCodec, Integer>      SORTIDS_AUDIO_CODEC;
    public static Map<FileContainer, Integer>   SORTIDS_FILE_CONTAINER;

    public static HashMap<String, Integer>      SUBTITLE_PREFERRENCE_MAP;

    static {
        updateSorterMaps();

        GenericConfigEventListener<String[]> listener = new GenericConfigEventListener<String[]>() {

            @Override
            public void onConfigValidatorError(KeyHandler<String[]> keyHandler, String[] invalidValue, ValidationException validateException) {
            }

            @Override
            public void onConfigValueModified(KeyHandler<String[]> keyHandler, String[] newValue) {
                updateSorterMaps();
            }

        };
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER.getEventSender().addListener(listener);
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER_AUDIO_BITRATE.getEventSender().addListener(listener);
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER_AUDIO_CODEC.getEventSender().addListener(listener);
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER_VIDEO_CODEC.getEventSender().addListener(listener);
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER_VIDEO_FRAMERATE.getEventSender().addListener(listener);
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER_RESOLUTION.getEventSender().addListener(listener);
        CFG_YOUTUBE.QUALITY_SORT_IDENTIFIER_ORDER_FILETYPE.getEventSender().addListener(listener);
    }

    private static void updateSorterMaps() {
        SORTIDS_VIDEO_RESOLUTION = update(VideoResolution.class, CFG.getQualitySortIdentifierOrderResolution());
        SORTIDS_VIDEO_CODEC = update(VideoCodec.class, CFG.getQualitySortIdentifierOrderVideoCodec());
        SORTIDS_VIDEO_FRAMERATE = update(VideoFrameRate.class, CFG.getQualitySortIdentifierOrderVideoFramerate());
        SORTIDS_AUDIO_BITRATE = update(AudioBitrate.class, CFG.getQualitySortIdentifierOrderAudioBitrate());
        SORTIDS_AUDIO_CODEC = update(AudioCodec.class, CFG.getQualitySortIdentifierOrderAudioCodec());
        SORTIDS_FILE_CONTAINER = update(FileContainer.class, CFG.getQualitySortIdentifierOrderFiletype());

        ArrayList<QualitySortIdentifier> sortIds = new ArrayList<QualitySortIdentifier>();
        for (String s : CFG.getQualitySortIdentifierOrder()) {
            sortIds.add(QualitySortIdentifier.valueOf(s));
        }
        SORTIDS = sortIds;
        updateSubtitlesSorter();

    }

    protected static void updateSubtitlesSorter() {
        HashMap<String, Integer> prefSubtitles = new HashMap<String, Integer>();
        String[] prefs = CFG_YOUTUBE.CFG.getPreferedSubtitleLanguages();
        int prefID = 0;

        if (prefs != null) {

            for (int i = 0; i < prefs.length; i++) {
                if (prefs[i] != null) {
                    prefSubtitles.put(prefs[i].toLowerCase(Locale.ENGLISH), prefID++);
                }

            }
        }
        prefSubtitles.put(TranslationFactory.getDesiredLanguage().toLowerCase(Locale.ENGLISH), prefID++);
        prefSubtitles.put(Locale.getDefault().getLanguage().toLowerCase(Locale.ENGLISH), prefID++);

        prefSubtitles.put("en", prefID++);

        SUBTITLE_PREFERRENCE_MAP = prefSubtitles;
    }

    private static <T extends Enum> Map<T, Integer> update(Class<T> class1, String[] values) {
        HashMap<T, Integer> ret = new HashMap<T, Integer>();

        List<T> lst = defaultEnumList(class1, values);
        for (int i = 0; i < lst.size(); i++) {
            ret.put(lst.get(i), lst.size() - i);

        }
        return ret;
    }

    public static <T extends Enum> List<T> defaultEnumList(Class<T> cls, String[] value) {

        String[] empty = new String[] {};
        if (value == null) {
            value = empty;

        }
        List<T> lst = new ArrayList<T>();
        HashSet<String> dupe = new HashSet<String>();

        for (String s : value) {
            try {
                Field field = cls.getDeclaredField(s);
                Object enumValue = field.get(null);
                if (enumValue != null) {
                    dupe.add(s);
                    lst.add((T) enumValue);
                }
            } catch (Throwable e) {

            }
        }
        for (Enum q : cls.getEnumConstants()) {
            if (!dupe.contains(q.name())) {
                try {
                    lst.add((T) q);
                } catch (Throwable e) {

                }
            }
        }
        return lst;
    }

}