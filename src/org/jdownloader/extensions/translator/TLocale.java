package org.jdownloader.extensions.translator;

import java.util.Locale;

import org.appwork.txtresource.TranslationFactory;

public class TLocale {

    private Locale locale;

    public Locale getLocale() {
        return locale;
    }

    public String getId() {
        return id;
    }

    private String id;

    public String toString() {
        boolean hasVariant = locale.getVariant().length() > 0;
        boolean hasCountry = locale.getCountry().length() > 0;

        final StringBuilder sb = new StringBuilder();
        sb.append(locale.getDisplayLanguage(Locale.ENGLISH));
        if (hasVariant || hasCountry) {
            sb.append(" (");
        }

        if (hasCountry) {
            sb.append(locale.getDisplayCountry(Locale.ENGLISH));
        }

        if (hasVariant) {
            if (hasCountry) sb.append(", ");
            sb.append(locale.getDisplayVariant(Locale.ENGLISH));

        }

        if (hasVariant || hasCountry) {
            sb.append(")");
        }
        return sb.toString();
    }

    public TLocale(String id) {
        locale = TranslationFactory.stringToLocale(id);
        this.id = id;
    }

}
