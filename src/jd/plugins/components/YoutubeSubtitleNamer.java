package jd.plugins.components;

import java.util.Locale;

import jd.plugins.DownloadLink;
import jd.plugins.decrypter.YoutubeHelper;

import org.jdownloader.gui.translate._GUI;

public class YoutubeSubtitleNamer implements YoutubeFilenameModifier {
    private static final YoutubeSubtitleNamer INSTANCE = new YoutubeSubtitleNamer();

    /**
     * get the only existing instance of YoutubeHelper.SubtitleNamer. This is a singleton
     * 
     * @return
     */
    public static YoutubeSubtitleNamer getInstance() {
        return YoutubeSubtitleNamer.INSTANCE;
    }

    /**
     * Create a new instance of YoutubeHelper.SubtitleNamer. This is a singleton class. Access the only existing instance by using
     * {@link #getInstance()}.
     */
    YoutubeSubtitleNamer() {

    }

    @Override
    public String run(String formattedFilename, DownloadLink link) {
        String code = link.getStringProperty(YoutubeHelper.YT_SUBTITLE_CODE, "");
        Locale locale = YoutubeHelper.forLanguageTag(code);
        formattedFilename = formattedFilename.replaceAll("\\*quality\\*", _GUI._.YoutubeDash_getName_subtitles_filename(locale.getDisplayName()));
        return formattedFilename;
    }
}