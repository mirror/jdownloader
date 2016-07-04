package org.jdownloader.plugins.components.youtube.keepForCompatibility;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.appwork.storage.JSonStorage;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;
import org.jdownloader.plugins.components.youtube.YoutubeClipData;
import org.jdownloader.plugins.components.youtube.itag.YoutubeITAG;
import org.jdownloader.plugins.components.youtube.variants.DownloadType;
import org.jdownloader.plugins.components.youtube.variants.VariantBase;
import org.jdownloader.plugins.components.youtube.variants.VariantGroup;
import org.jdownloader.plugins.components.youtube.variants.YoutubeBasicVariantStorable;

import org.appwork.utils.parser.UrlQuery;
import jd.http.Request;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class SubtitleVariantOld {
    private final Icon TEXT = new AbstractIcon(IconKey.ICON_TEXT, 16);
    private Locale     locale;
    private String     code;
    private String     source;
    private String     identifier;
    private String     kind;

    @Deprecated
    public SubtitleVariantOld(String identifier) {
        this.identifier = identifier;

        UrlQuery qi;
        try {
            qi = Request.parseQuery(identifier);

            this.code = qi.get("lng");
            this.source = qi.get("src");
            this.kind = qi.get("kind");
        } catch (Throwable e) {
            e.printStackTrace();
        }
        if (StringUtils.isEmpty(code)) {
            code = identifier;
        }
        locale = TranslationFactory.stringToLocale(this.code);
    }

    public Locale _getLocale() {
        return locale;
    }

    public String _getCode() {
        return code;
    }

    public String _getTooltipDescription(Object caller) {
        return _getExtendedName(caller);
    }

    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SubtitleVariantOld)) {
            return false;
        }
        return code.equals(((SubtitleVariantOld) obj).code);
    }

    public int hashCode() {
        return code.hashCode();
    }

    public String _getName(Object caller) {
        StringBuilder sb = new StringBuilder();
        if (_isTranslated()) {
            sb.append(_GUI.T.lit_translated());
        }
        if (_isSpeechToText()) {
            if (sb.length() > 0) {
                sb.append("; ");
            }
            sb.append(_GUI.T.lit_speedtotext());
        }

        if (sb.length() == 0) {
            return _GUI.T.YoutubeDash_getName_subtitles_(locale.getDisplayName());
        } else {
            return _GUI.T.YoutubeDash_getName_subtitles_annotated(locale.getDisplayName(), sb.toString());
        }

    }

    private boolean _isSpeechToText() {
        return "asr".equalsIgnoreCase(kind);
    }

    public Icon _getIcon(Object caller) {
        return TEXT;
    }

    public String getFileExtension() {
        return "srt";
    }

    public String _getUniqueId() {
        return code;
    }

    public String getMediaTypeID() {
        return VariantGroup.SUBTITLES.name();
    }

    public YoutubeITAG getiTagVideo() {
        return null;
    }

    public YoutubeITAG getiTagAudio() {
        return null;
    }

    public YoutubeITAG getiTagData() {
        return null;
    }

    public double getQualityRating() {
        return 0;
    }

    public String getTypeId() {
        return code;
    }

    public DownloadType getType() {
        return DownloadType.SUBTITLES;
    }

    public VariantGroup getGroup() {
        return VariantGroup.SUBTITLES;
    }

    public void convert(DownloadLink downloadLink, PluginForHost plugin) {
    }

    public String getQualityExtension(Object caller) {
        return code;
    }

    public String modifyFileName(String formattedFilename, DownloadLink link) {
        return formattedFilename;
    }

    public boolean hasConverter(DownloadLink downloadLink) {
        return false;
    }

    public List<File> listProcessFiles(DownloadLink link) {
        return null;
    }

    public String _getExtendedName(Object caller) {
        return _GUI.T.YoutubeDash_getName_subtitles_(locale.getDisplayName());
    }

    public boolean _isTranslated() {

        return StringUtils.isNotEmpty(source);
    }

    public String _getIdentifier() {
        return identifier;
    }

    public boolean isValidFor(YoutubeClipData vid) {
        return vid.subtitles != null && vid.subtitles.size() > 0;
    }

    public String getStorableString() {
        YoutubeBasicVariantStorable storable = new YoutubeBasicVariantStorable();
        storable.setId(VariantBase.SUBTITLES.name());
        return JSonStorage.serializeToJson(storable);
    }

}
