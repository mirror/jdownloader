package org.jdownloader.plugins.components.youtube.variants;

import java.util.List;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.YoutubeConfig;
import org.jdownloader.plugins.components.youtube.YoutubeStreamData;

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
    public void setJson(String jsonString) {
        setGenericInfo(JSonStorage.restoreFromString(jsonString, YoutubeSubtitleStorable.TYPE));
    }

    @Override
    protected void fill(YoutubeClipData vid, List<YoutubeStreamData> audio, List<YoutubeStreamData> video, List<YoutubeStreamData> data) {
    }

    private static final Icon TEXT = new AbstractIcon(IconKey.ICON_TEXT, 16);

    @Override
    public Icon _getIcon(Object caller) {
        return TEXT;
    }

    @Override
    public String toString() {
        return _getName(null);
    }

    @Override
    public String getFileNamePattern() {
        return JsonConfig.create(YoutubeConfig.class).getSubtitleFilenamePattern();
    }

    @Override
    public String _getName(Object caller) {

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
            return _GUI.T.YoutubeDash_getName_subtitles_(getGenericInfo()._getLocale().getDisplayName());
        } else {
            return _GUI.T.YoutubeDash_getName_subtitles_annotated(getGenericInfo()._getLocale().getDisplayName(), sb.toString());
        }
    }

    @Override
    public double getQualityRating() {
        double ret = super.getQualityRating();
        if (getGenericInfo()._isTranslated()) {
            ret /= 2;
        }
        if (getGenericInfo()._isSpeechToText()) {
            ret /= 3;
        }
        return ret;
    }

    @Override
    public String getFileNameQualityTag() {
        return getGenericInfo()._getLocale().getDisplayLanguage();
    }

}
