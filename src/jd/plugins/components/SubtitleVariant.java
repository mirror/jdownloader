package jd.plugins.components;

import java.io.File;
import java.util.List;
import java.util.Locale;

import javax.swing.Icon;

import jd.plugins.DownloadLink;
import jd.plugins.PluginForHost;

import org.appwork.txtresource.TranslationFactory;
import org.jdownloader.gui.IconKey;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.images.AbstractIcon;

public class SubtitleVariant implements YoutubeVariantInterface {
    private final Icon TEXT = new AbstractIcon(IconKey.ICON_TEXT, 16);
    private Locale     locale;
    private String     code;

    public SubtitleVariant(String code) {
        this.code = code;
        locale = TranslationFactory.stringToLocale(code);
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
        return _GUI._.YoutubeDash_getName_subtitles_(locale.getDisplayName());
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
        return _GUI._.YoutubeDash_getName_subtitles_(locale.getDisplayName());
    }

}
