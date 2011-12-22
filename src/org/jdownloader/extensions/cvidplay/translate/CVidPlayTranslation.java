package org.jdownloader.extensions.cvidplay.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.TranslateInterface;

public interface CVidPlayTranslation extends TranslateInterface {
    @Default(lngs = { "en" }, values = { "Media Player" })
    String title();

    @Default(lngs = { "en" }, values = { "Play Media Files in JDownloader" })
    String description();

    @Default(lngs = { "en" }, values = { "Media Player" })
    String gui_title();

    @Default(lngs = { "en" }, values = { "Play Media Files in JDownloader" })
    String gui_tooltip();

}
