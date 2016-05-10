package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.utils.CompareUtils;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.plugins.components.youtube.YT_STATICS;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;
import org.jdownloader.plugins.config.PluginJsonConfig;

public class SubtitleVariant extends AbstractVariant<YoutubeSubtitleStorable> {
    // public String getCustomName(Object caller) {
    // return _GUI.T.YoutubeVariant_name_SUBTITLES();
    // }
    //
    // @Override
    // public String getCustomQualityExtension() {
    // return _GUI.T.YoutubeVariant_filenametag_SUBTITLES();
    // }
    public SubtitleVariant(YoutubeSubtitleStorable si) {
        this();
        setGenericInfo(si);

    }

    public SubtitleVariant() {
        super(VariantBase.SUBTITLES);

    }

    @Override
    public String _getUniqueId() {
        return getGenericInfo()._getUniqueId();
    }

    @Override
    public String createAdvancedName() {
        String ret = "SUBTITLE " + (getGenericInfo()._getLocale() == null ? getGenericInfo().getLanguage() : getGenericInfo()._getLocale().getDisplayName(Locale.ENGLISH));
        if (StringUtils.isNotEmpty(getGenericInfo().getKind())) {
            ret += ", Kind " + getGenericInfo().getKind();
        }
        if (StringUtils.isNotEmpty(getGenericInfo().getSourceLanguage())) {
            ret += ", Source " + getGenericInfo().getSourceLanguage();
        }
        return ret;
    }

    @Override
    public void setJson(String jsonString) {
        setGenericInfo(JSonStorage.restoreFromString(jsonString, YoutubeSubtitleStorable.TYPE));
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
    }

    @Override
    public Icon _getIcon(Object caller) {
        return getGroup().getIcon(18);
    }

    @Override
    public String toString() {
        return _getName(null);
    }

    @Override
    public String getFileNamePattern() {
        return PluginJsonConfig.get(YoutubeConfig.class).getSubtitleFilenamePattern();
    }

    @Override
    public String _getName(Object caller) {
        if (getGenericInfo() == null || StringUtils.isEmpty(getGenericInfo().getLanguage())) {
            return _GUI.T.YoutubeBasicVariant_getLabel_subtitles();
        }
        StringBuilder sb = new StringBuilder();
        if (getGenericInfo()._isTranslated()) {
            sb.append(_GUI.T.lit_translated());
        }
        if (getGenericInfo()._isSpeechToText()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(_GUI.T.lit_speedtotext());
        }

        if (sb.length() == 0) {
            return _GUI.T.YoutubeDash_getName_subtitles_(getGenericInfo()._getLocale() == null ? getGenericInfo().getLanguage() : getGenericInfo()._getLocale().getDisplayName());
        } else {
            return _GUI.T.YoutubeDash_getName_subtitles_annotated(getGenericInfo()._getLocale() == null ? getGenericInfo().getLanguage() : getGenericInfo()._getLocale().getDisplayName(), sb.toString());
        }
    }

    // @Override
    // public double getQualityRating() {
    // double ret = super.getQualityRating();
    // if (getGenericInfo()._isTranslated()) {
    // ret /= 2;
    // }
    // if (getGenericInfo()._isSpeechToText()) {
    // ret /= 3;
    // }
    // return ret;
    // }
    @Override
    public int compareTo(Object o) {

        AbstractVariant o1 = this;
        AbstractVariant o2 = (AbstractVariant) o;
        int ret = 0;

        if (o1 instanceof SubtitleVariant && o2 instanceof SubtitleVariant) {
            Integer pref1 = YT_STATICS.SUBTITLE_PREFERRENCE_MAP.get(((SubtitleVariant) o1).getLanguageCode());
            Integer pref2 = YT_STATICS.SUBTITLE_PREFERRENCE_MAP.get(((SubtitleVariant) o2).getLanguageCode());
            if (pref1 == null) {
                pref1 = Integer.MAX_VALUE;
            }
            if (pref2 == null) {
                pref2 = Integer.MAX_VALUE;
            }
            ret = CompareUtils.compare(pref2, pref1);

            if (ret == 0) {

                ret = CompareUtils.compare(((SubtitleVariant) o1).getGenericInfo()._isSpeechToText(), ((SubtitleVariant) o2).getGenericInfo()._isSpeechToText());

            }
            if (ret == 0) {

                ret = CompareUtils.compare(((SubtitleVariant) o1).getGenericInfo()._isTranslated(), ((SubtitleVariant) o2).getGenericInfo()._isTranslated());

            }

            if (ret == 0) {
                ret = CompareUtils.compare(((SubtitleVariant) o2).getDisplayLanguage(), ((SubtitleVariant) o1).getDisplayLanguage());
            }

            return ret;

        }
        return -1;
    }

    public String getDisplayLanguage() {
        return getGenericInfo()._getLocale().getDisplayLanguage();
    }

    @Override
    public String getFileNameQualityTag() {
        return getGenericInfo()._getLocale().getDisplayLanguage();
    }

    @Override
    public String getTypeId() {
        return _getUniqueId();

    }

    public String getLanguageCode() {
        return getGenericInfo()._getLocale().getLanguage();
    }

}
