package org.jdownloader.extensions.webinterface;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface WebinterfaceTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Webinterface" })
    String title();

}
