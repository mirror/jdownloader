package org.jdownloader.plugins.components.youtube.variants;

import java.io.UnsupportedEncodingException;
import java.util.Locale;

import org.appwork.storage.Storable;
import org.appwork.storage.TypeRef;
import org.appwork.txtresource.TranslationFactory;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.URLEncode;
import org.jdownloader.plugins.components.youtube.variants.generics.AbstractGenericVariantInfo;

public class YoutubeSubtitleStorable extends AbstractGenericVariantInfo implements Storable {
    public static final TypeRef<YoutubeSubtitleStorable> TYPE = new TypeRef<YoutubeSubtitleStorable>(YoutubeSubtitleStorable.class) {
    };

    public YoutubeSubtitleStorable(/* Storable */) {
    }

    public boolean _isTranslated() {
        return StringUtils.isNotEmpty(sourceLanguage);
    }

    private String language;
    private Locale locale;

    public Locale _getLocale() {
        return locale;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
        locale = TranslationFactory.stringToLocale(language);
    }

    public String getSourceLanguage() {
        return sourceLanguage;
    }

    public void setSourceLanguage(String sourceLanguage) {
        this.sourceLanguage = sourceLanguage;
    }

    private String sourceLanguage;

    public String getBase() {
        return base;
    }

    public void setBase(String base) {
        this.base = base;
    }

    public void setKind(String kind) {
        this.kind = kind;
    }

    private String kind;
    private String base;
    private String name;

    public YoutubeSubtitleStorable(String base, String name, String language, String source, String kind) {
        this.base = base;
        setLanguage(language);
        this.sourceLanguage = source;
        this.kind = kind;
        this.name = name;
    }

    public String createUrl() {
        final StringBuilder url = new StringBuilder();
        url.append(base);
        if (StringUtils.isNotEmpty(sourceLanguage)) {
            url.append("&lang=").append(encode(sourceLanguage));
        }
        url.append("&lang=").append(encode(language));
        if (StringUtils.isNotEmpty(kind)) {
            url.append("&kind=").append(encode(kind));
        }
        if (StringUtils.isNotEmpty(name)) {
            url.append("&name=").append(encode(name));
        }
        return url.toString();
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

    public String _getUniqueId() {
        if (language == null) {
            return null;
        }
        final StringBuilder url = new StringBuilder();
        url.append("&lng=").append(encode(language));
        if (StringUtils.isNotEmpty(sourceLanguage)) {
            url.append("&src=").append(encode(sourceLanguage));
        }
        if (StringUtils.isNotEmpty(kind)) {
            url.append("&kind=").append(encode(kind));
        }
        if (StringUtils.isNotEmpty(name)) {
            url.append("&name=").append(encode(name));
        }
        return url.toString();
    }

    public boolean _isSpeechToText() {
        return kind != null && "asr".equalsIgnoreCase(kind);
    }
}