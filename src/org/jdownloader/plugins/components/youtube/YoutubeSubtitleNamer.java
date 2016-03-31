package org.jdownloader.plugins.components.youtube;

import java.net.MalformedURLException;
import java.util.Locale;

import org.appwork.exceptions.WTFException;
import org.appwork.txtresource.TranslationFactory;
import org.jdownloader.gui.translate._GUI;

import jd.http.Request;
import jd.plugins.DownloadLink;

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
        String code = link.getStringProperty("YT_SUBTITLE_CODE", "");
        Locale locale;
        try {
            locale = TranslationFactory.stringToLocale(Request.parseQuery(code).get("lng"));
        } catch (MalformedURLException e) {
            throw new WTFException(e);
        }
        formattedFilename = formattedFilename.replaceAll("\\*quality\\*", _GUI.T.YoutubeDash_getName_subtitles_filename(locale.getDisplayName()));
        return formattedFilename;
    }
}