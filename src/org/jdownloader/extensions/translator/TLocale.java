package org.jdownloader.extensions.translator;

import java.util.Locale;

import org.appwork.txtresource.TranslationFactory;

/**
 * Wrapper for a {@link #locale} instance.
 * 
 * @author thomas
 * 
 */
public class TLocale {

    private Locale locale;

    public Locale getLocale() {
        return locale;
    }

    public static void main(String[] a) {
        // get the default locale
        Locale l = Locale.getDefault();
        System.out.println("   Language, Country, Variant, Name");
        System.out.println("");
        System.out.println("Default locale: ");
        System.out.println("   " + l.getLanguage() + ", " + l.getCountry() + ", " + ", " + l.getVariant() + ", " + l.getDisplayName());
        // get a predefined locale
        l = Locale.CANADA_FRENCH;
        System.out.println("A predefined locale - Locale.CANADA_FRENCH:");
        System.out.println("   " + l.getLanguage() + ", " + l.getCountry() + ", " + ", " + l.getVariant() + ", " + l.getDisplayName());
        // define a new locale
        l = new Locale("en", "CN");
        System.out.println("User defined locale -Locale(\"en\",\"CN\"):");
        System.out.println("   " + l.getLanguage() + ", " + l.getCountry() + ", " + ", " + l.getVariant() + ", " + l.getDisplayName());
        // define another new locale
        l = new Locale("ll", "CC");
        System.out.println("User defined locale -Locale(\"ll\",\"CC\"):");
        System.out.println("   " + l.getLanguage() + ", " + l.getCountry() + ", " + ", " + l.getVariant() + ", " + l.getDisplayName());
        // get the supported locales
        Locale[] s = Locale.getAvailableLocales();
        System.out.println("Supported locales: ");
        for (int i = 0; i < s.length; i++) {
            System.out.println("   " + s[i].getLanguage() + ", " + s[i].getCountry() + ", " + s[i].getVariant() + ", " + s[i].getDisplayName());
        }
    }

    public String getId() {
        return id;
    }

    private String id;

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof TLocale) { return ((TLocale) obj).id.equals(id); }
        return false;

    }

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
