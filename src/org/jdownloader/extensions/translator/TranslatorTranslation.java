package org.jdownloader.extensions.translator;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface TranslatorTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Translator" })
    public String Translator();

    @Default(lngs = { "en" }, values = { "This Extension can be used to edit JDownloader translations. You need a developer account to use this extension" })
    public String description();
}
