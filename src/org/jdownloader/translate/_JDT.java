package org.jdownloader.translate;

import org.appwork.txtresource.TranslationFactory;

public class _JDT {

    public static String getLanguage() {
        return TranslationFactory.getDesiredLanguage();
    }

    public static final JdownloaderTranslation _ = TranslationFactory.create(JdownloaderTranslation.class);

}
