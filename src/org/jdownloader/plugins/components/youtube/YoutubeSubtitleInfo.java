package org.jdownloader.plugins.components.youtube;

import java.io.UnsupportedEncodingException;

import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;

public class YoutubeSubtitleInfo {

    private String _base;

    public YoutubeSubtitleInfo(/* Storable */) {

    }

    private String language;

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    private String sourceLanguage;
    private String kind;

    public YoutubeSubtitleInfo(String ttsUrl, String language, String source, String kind) {
        this._base = ttsUrl;
        this.language = language;
        this.sourceLanguage = source;
        this.kind = kind;

    }

    public String _getUrl(String videoId) throws UnsupportedEncodingException {
        String ret = "";
        if (StringUtils.isNotEmpty(sourceLanguage)) {
            ret = _base + "&lang=" + encode(sourceLanguage) + "&tlang=" + encode(language);
        } else {
            ret = _base + "&lang=" + encode(language);
        }
        if (StringUtils.isNotEmpty(kind)) {
            ret += "&kind=" + encode(kind);
        }
        return ret;

    }

    public String _getIdentifier() {

        String ret = "&lng=" + encode(language);
        if (StringUtils.isNotEmpty(sourceLanguage)) {
            ret += "&src=" + encode(sourceLanguage);
        }
        if (StringUtils.isNotEmpty(kind)) {
            ret += "&kind=" + encode(kind);
        }
        return ret;

    }

    private String encode(String kind2) {
        try {
            return URLEncode.encodeRFC2396(kind2);
        } catch (UnsupportedEncodingException e) {
            return kind2;
        }
    }

    public String getKind() {
        return kind;
    }

}