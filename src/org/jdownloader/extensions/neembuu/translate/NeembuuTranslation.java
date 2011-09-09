package org.jdownloader.extensions.neembuu.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface NeembuuTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Neembuu" })
    String title();

    @Default(lngs = { "en" }, values = { "Stream Data - Watch as you download" })
    String description();

    @Default(lngs = { "en" }, values = { "Neembuu" })
    String gui_title();

    @Default(lngs = { "en" }, values = { "Stream Data - Watch as you download" })
    String gui_tooltip();

}
