package jd.plugins.components;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

import jd.http.QueryInfo;
import jd.http.Request;
import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

public class SubtitleVariant implements YoutubeVariantInterface {
    private final Icon TEXT = new AbstractIcon(IconKey.ICON_TEXT, 16);
    private Locale     locale;
    private String     code;
    private String     source;
    private String     identifier;
    private String     kind;

    public SubtitleVariant(String identifier) {
        this.identifier = identifier;

        QueryInfo qi;
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

    @Override
    public String _getTooltipDescription() {
        return _getExtendedName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !(obj instanceof SubtitleVariant)) {
            return false;
        }
        return code.equals(((SubtitleVariant) obj).code);
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    public String _getName() {
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

    public Icon _getIcon() {
        return TEXT;
    }

    @Override
    public String getFileExtension() {
        return "srt";
    }

    @Override
    public String _getUniqueId() {
        return code;
    }

    @Override
    public String getMediaTypeID() {
        return VariantGroup.SUBTITLES.name();
    }

    @Override
    public YoutubeITAG getiTagVideo() {
        return null;
    }

    @Override
    public YoutubeITAG getiTagAudio() {
        return null;
    }

    @Override
    public YoutubeITAG getiTagData() {
        return null;
    }

    @Override
    public double getQualityRating() {
        return 0;
    }

    @Override
    public String getTypeId() {
        return code;
    }

    @Override
    public DownloadType getType() {
        return DownloadType.SUBTITLES;
    }

    @Override
    public VariantGroup getGroup() {
        return VariantGroup.SUBTITLES;
    }

    @Override
    public void convert(DownloadLink downloadLink, PluginForHost plugin) {
    }

    @Override
    public String getQualityExtension() {
        return code;
    }

    @Override
    public String modifyFileName(String formattedFilename, DownloadLink link) {
        return formattedFilename;
    }

    @Override
    public boolean hasConverter(DownloadLink downloadLink) {
        return false;
    }

    @Override
    public List<File> listProcessFiles(DownloadLink link) {
        return null;
    }

    @Override
    public String _getExtendedName() {
        return _GUI.T.YoutubeDash_getName_subtitles_(locale.getDisplayName());
    }

    public boolean _isTranslated() {

        return StringUtils.isNotEmpty(source);
    }

    public String _getIdentifier() {
        return identifier;
    }

}
