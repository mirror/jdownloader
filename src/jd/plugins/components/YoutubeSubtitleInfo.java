package jd.plugins.components;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class YoutubeSubtitleInfo {

    private String _base;

    public String getLang() {
        return lang;
    }

    public String getName() {
        return name;
    }

    public String getKind() {
        return kind;
    }

    public String getLangOrg() {
        return langOrg;
    }

    private String lang;

    public void setLang(String lang) {
        this.lang = lang;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    public void setLangOrg(String langOrg) {
        this.langOrg = langOrg;
    }

    private String name;
    private String kind;
    private String langOrg;

    public YoutubeSubtitleInfo(/* Storable */) {

    }

    public YoutubeSubtitleInfo(String ttsUrl, String lang, String name, String kind, String langOrg) {
        this._base = ttsUrl;
        this.lang = lang;
        this.name = name;
        this.kind = kind;
        this.langOrg = langOrg;
    }

    public String _getUrl(String videoId) throws UnsupportedEncodingException {
        return _base + "&kind=" + URLEncoder.encode(kind, "UTF-8") + "&format=1&ts=" + System.currentTimeMillis() + "&type=track&lang=" + URLEncoder.encode(lang, "UTF-8") + "&name=" + URLEncoder.encode(name, "UTF-8") + "&v=" + URLEncoder.encode(videoId, "UTF-8");
    }

}