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

    @Default(lngs = { "en", "de" }, values = { "Close Translator", "Translator schließen" })
    String TranslatorExtensionGuiToggleAction_selected();

    @Default(lngs = { "en", "de" }, values = { "Close the Translator Tab", "Den Translator Reiter schließen" })
    String TranslatorExtensionGuiToggleAction_selected_tt();

    @Default(lngs = { "en", "de" }, values = { "Open Translator", "Translator öffnen" })
    String TranslatorExtensionGuiToggleAction_deselected();

    @Default(lngs = { "en", "de" }, values = { "Open the Translator Tab", "Den Translator Reiter öffnen" })
    String TranslatorExtensionGuiToggleAction_deselected_tt();
}
