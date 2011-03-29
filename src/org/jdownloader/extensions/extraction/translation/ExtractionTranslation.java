package org.jdownloader.extensions.extraction.translation;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface ExtractionTranslation extends TranslateInterface {
    @Default(lngs = { "en", "de" }, values = { "Archive Extractor", "Archiv Entpacker" })
    String name();

    @Default(lngs = { "en", "de" }, values = { "Extracts all usual types of archives (zip,rar,7zip,...)", "Entpackt all üblichen Archivtypen (zip,rar,7zip,..)" })
    String description();

}
