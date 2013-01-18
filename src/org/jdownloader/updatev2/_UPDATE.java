package org.jdownloader.updatev2;

import java.io.IOException;
import java.net.URISyntaxException;

import org.appwork.txtresource.TranslationFactory;
import org.appwork.txtresource.TranslationUtils;
import org.appwork.utils.locale.AWUTranslation;

public class _UPDATE {
    public static final UpdaterTranslation _ = TranslationFactory.create(UpdaterTranslation.class);

    public static void main(final String[] args) throws URISyntaxException, IOException {
        TranslationUtils.createFiles(false, UpdaterTranslation.class, AWUTranslation.class);
    }
}