package org.jdownloader.extensions.newWebinterface.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface newWebinterfaceTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "JDownloader Webinterface" })
    String title();

    @Default(lngs = { "en" }, values = { "Hello %s2, how are you? Firstname: %s2 Lastname: %s1 Do you See this '%s3'?" })
    String test_translation_func();
}