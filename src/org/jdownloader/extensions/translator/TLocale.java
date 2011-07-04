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

        final StringBuilder sb = new StringBuilder();
        sb.append(locale.getDisplayLanguage(Locale.ENGLISH));
        if (hasVariant) {
            sb.append(" (");
        }

        if (hasVariant) {

            String v = locale.getDisplayVariant(Locale.ENGLISH);
            if (v.equals(locale.getVariant())) {
                v = new Locale(locale.getLanguage(), v, v).getDisplayCountry(Locale.ENGLISH);
            }
            sb.append(v);

        }

        if (hasVariant) {
            sb.append(")");
        }
        return sb.toString();
    }

    public TLocale(String id) {
        locale = TranslationFactory.stringToLocale(id);
        this.id = id;
    }

}
