package org.jdownloader.translate;

import java.io.IOException;
import java.net.URISyntaxException;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.txtresource.TranslationUtils;

public class JDT {
    public static final JDTranslation _ = TranslationFactory.create(JDTranslation.class);

    @SuppressWarnings("unchecked")
    public static void main(final String[] args) throws URISyntaxException, IOException {
        TranslationUtils.createFiles(false, JDTranslation.class);
    }

    public static String getLanguage() {
        return TranslationFactory.getDesiredLanguage();
    }

}
