package org.jdownloader.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface JDTranslation extends TranslateInterface {
    //

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for Installation. Do you want to run the update now?", "%s1 Update(s) können jetzt installiert werden. Wollen Sie jetzt aktualisieren? " })
    String update_dialog_msg_x_updates_available(int num);

    @Default(lngs = { "en", "de" }, values = { "Update(s) available", "Update(s) verfügbar" })
    String update_dialog_title_updates_available();

    @Default(lngs = { "en", "de" }, values = { "Yes(recommended)", "Ja(empfohlen)" })
    String update_dialog_yes();

    @Default(lngs = { "en", "de" }, values = { "Ask me later", "Später erneut fragen" })
    String update_dialog_later();

    @Default(lngs = { "en", "de" }, values = { "Error occured!", "Fehler aufgetreten!" })
    String dialog_title_exception();

    @Default(lngs = { "en", "de" }, values = { "Tools", "Tools" })
    String gui_menu_extensions();

    @Default(lngs = { "en", "de" }, values = { "Window", "Fenster" })
    String gui_menu_windows();

    @Default(lngs = { "en", "de" }, values = { "Restart Required", "Neustart nötig" })
    String dialog_optional_showRestartRequiredMessage_title();

    @Default(lngs = { "en", "de" }, values = { "Your changes require a JDownloader restart to take effect. Restart now?", "Ihre Änderungen benötigen einen Neustart von JDownloader. Jetzt neu starten?" })
    String dialog_optional_showRestartRequiredMessage_msg();

    @Default(lngs = { "en", "de" }, values = { "Yes", "Ja" })
    String basics_yes();

    @Default(lngs = { "en", "de" }, values = { "No", "Nein" })
    String basics_no();

    @Default(lngs = { "en", "de" }, values = { "Show %s1 now?\r\nYou may open it later using Mainmenu->Window", "%s1 jetzt anzeigen?\r\n%s1 kann jederzeit über Hauptmenü -> Fenster angezeigt werden." })
    String gui_settings_extensions_show_now(String name);

    @Default(lngs = { "en", "de" }, values = { "General", "Allgemein" })
    String gui_settings_general_title();

    @Default(lngs = { "en", "de" }, values = { "Choose", "Auswählen" })
    String basics_browser_folder();

    @Default(lngs = { "en", "de" }, values = { "Choose directory", "Ordner auswählen" })
    String gui_setting_folderchooser_title();

    @Default(lngs = { "en", "de" }, values = { "If a Proxy Server is required to access internet, please enter proxy data here. JDownloader is able to rotate several Proxies to avoid IP waittimes.", "Falls ein Proxy benötigt wird um ins Internet zu verbinden, kann dieser hier eingetragen werden. Um IP Wartezeit zu vermeiden, können mehrere Proxy Server eingetragen werden." })
    String gui_settings_proxy_description();

    @Default(lngs = { "en", "de" }, values = { "Add", "Neu" })
    String basics_add();

    @Default(lngs = { "en", "de" }, values = { "Remove", "Entfernen" })
    String basics_remove();

    @Default(lngs = { "en", "de" }, values = { "Set the default download path here. Changing default path here, affects only new downloads.", "Standard Download Zielordner setzen. Eine Änderung betrifft nur neue Links." })
    String gui_settings_downloadpath_description();

    @Default(lngs = { "en", "de" }, values = { "Using the hashcheck option, JDownloader to verify your downloads for correctness after download.", "Über den automatischen Hashcheck kann JDownloader die geladenen Dateien automatisch auf Korrektheit überprüfen." })
    String gui_settings_filewriting_description();

}
