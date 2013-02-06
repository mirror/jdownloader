package org.jdownloader.updatev2;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface UpdaterTranslation extends TranslateInterface {

    ;

    @Default(lngs = { "en", "de" }, values = { "Cancel", "Abbrechen" })
    String update_dialog_cancel();

    @Default(lngs = { "en", "de" }, values = { "Ask me later", "Später erneut fragen" })
    String update_dialog_later();

    @Default(lngs = { "en", "de" }, values = { "Updates are ready for Installation. Do you want to run the update now?", "Updates können jetzt installiert werden. Soll das Update jetzt gestartet werden?" })
    String update_dialog_msg_x_updates_available();

    @Default(lngs = { "en", "de" }, values = { "Open Changelog", "Änderungen ansehen" })
    String update_dialog_news_button();

    @Default(lngs = { "en", "de" }, values = { "http://www.jdownloader.org/changelog", "http://www.jdownloader.org/changelog" })
    String update_dialog_news_button_url();

    @Default(lngs = { "en", "de" }, values = { "Update(s) available", "Update(s) verfügbar" })
    String update_dialog_title_updates_available();

    @Default(lngs = { "en", "de" }, values = { "Yes(recommended)", "Ja(empfohlen)" })
    String update_dialog_yes();

    @Default(lngs = { "en", "de" }, values = { "You already have the latest version", "Sie nutzen bereits die aktuelle Version." })
    String guiless_you_are_up2date();

    @Default(lngs = { "en", "de" }, values = { "Are you sure that you want to exit?", "Soll die Anwendung wirklich beendet werden?" })
    String rlyexit();

    @Default(lngs = { "en", "de" }, values = { "Really exit?", "Wirklich beenden?" })
    String rlyexit_title();

    ;

    @Default(lngs = { "en", "de" }, values = { "Later", "Später" })
    String confirmdialog_new_update_available_answer_later();

    @Default(lngs = { "en", "de" }, values = { "Later", "Später" })
    String confirmdialog_new_update_available_answer_later_install();

    @Default(lngs = { "en", "de" }, values = { "Download now", "Jetzt herunterladen" })
    String confirmdialog_new_update_available_answer_now();

    @Default(lngs = { "en", "de" }, values = { "Install now", "Jetzt installieren" })
    String confirmdialog_new_update_available_answer_now_install();

    @Default(lngs = { "en", "de" }, values = { "New Update available", "Ein neues Update ist verfügbar" })
    String confirmdialog_new_update_available_frametitle();

    @Default(lngs = { "en", "de" }, values = { "A new Update is available. Do you want to download it now?", "Ein neues Update steht zum Download bereit. Soll es jetzt geladen werden?" })
    String confirmdialog_new_update_available_message();

    @Default(lngs = { "en", "de" }, values = { "A new JDownloader Update is available. To install, a restart is required.\r\nDo you want to restart & install the update now?", "Ein neues JDownloader Update steht zur Installation bereit. Zur Installation muss ein Neustart durchgeführt werden.\r\nSoll jetzt neu gestartet und installiert werden?\r\n" })
    String confirmdialog_new_update_available_for_install_message();

    @Default(lngs = { "en", "de" }, values = { "An Error occured", "Ein Fehler ist aufgetreten" })
    String errordialog_frametitle();

    @Default(lngs = { "en", "de" }, values = { "An unexpected error occured during the update.\r\nPlease try again or contact our support.", "Während dem Update ist ein Fehler aufgetreten.\r\nVersuchen Sie es erneut, oder kontaktieren Sie unseren Support." })
    String errordialog_message();

    @Default(lngs = { "en", "de" }, values = { "JDownloader could not connect to the Updateserver.\r\nPlease make sure that you are connected to the Internet.", "JDownloader konnte keine Verbindung zum Updateserver herstellen.\r\nBitte stellen Sie sicher, dass Sie mit dem Internet verbunden sind." })
    String errordialog_noconnection();

    @Default(lngs = { "en", "de" }, values = { "The Update Server is not available right now. Please try again later or contact our support", "Der Updateserver ist momentan nicht erreichbar. Bitte versuchen Sie es später erneut oder kontaktieren Sie unseren Support." })
    String errordialog_server_error();

    @Default(lngs = { "en", "de" }, values = { "The Update Server is busy right now. Please try again in a few minutes.", "Der Updateserver ist ausgelastet. Bitte versuchen Sie es in einigen Minuten erneut." })
    String errordialog_server_locked();

    @Default(lngs = { "en", "de" }, values = { "The Update Server is not available right now. Please try again later.", "Der Updateserver ist momentan nicht erreichbar. Bitte versuchen Sie es später erneut." })
    String errordialog_server_offline();

    @Default(lngs = { "en", "de" }, values = { "JDownloader Updater", "JDownloader Updater" })
    String installframe_frametitle();

    @Default(lngs = { "en", "de" }, values = { "Installation complete...", "Installation abgeschlossen..." })
    String installframe_statusmsg_complete();

    @Default(lngs = { "en", "de" }, values = { "Download Updates...", "Lade Updates herunter..." })
    String installframe_statusmsg_download();

    @Default(lngs = { "en", "de" }, values = { "Download Updates...\r\nDownloadspeed: %s1/s, Time left: %s2", "Lade Updates herunter...\r\nDownload Geschwindigkeit: %s1/s, Verbleibende Zeit: %s2" })
    String installframe_statusmsg_downloadspeed(String formatBytes, String eta);

    @Default(lngs = { "en", "de" }, values = { "Finalizing installation...", "Die Installation wird fertig gestellt..." })
    String installframe_statusmsg_finalizing();

    @Default(lngs = { "en", "de" }, values = { "Check for updates", "Nach Updates suchen" })
    String installframe_statusmsg_findupdates();

    @Default(lngs = { "en", "de" }, values = { "Installation in progress - please wait...", "Die Installation läuft. Bitte warten Sie..." })
    String installframe_statusmsg_installing();

    @Default(lngs = { "en", "de" }, values = { "Update has been interrupted", "Das Update wurde unterbrochen" })
    String installframe_statusmsg_interrupted();

    @Default(lngs = { "en", "de" }, values = { "Preparing for installation...", "Die Installation wird vorbereitet..." })
    String installframe_statusmsg_prepare();

    @Default(lngs = { "en", "de" }, values = { "Prepare Update package...", "Update Paket vorbereiten..." })
    String installframe_statusmsg_preparing();

    @Default(lngs = { "en", "de" }, values = { "Reverting all Changes...\r\nPlease wait until all changes have been reverted.", "Änderungen zurücksetzen...\r\nBitte warten Sie bis alle Änderungen rückgängig gemacht wurden." })
    String installframe_statusmsg_reverting();

    @Default(lngs = { "en", "de" }, values = { "New JDownloader Version found...", "Neue JDownloader Version gefunden..." })
    String installframe_statusmsg_selfupdate();

    @Default(lngs = { "en", "de" }, values = { "Validing Installation.\r\nThis can take up to 5 minutes...", "Installation wird validiert.\r\nDies kann bis zu 5 Minuten dauern." })
    String installframe_statusmsg_selfupdate_validating();

    @Default(lngs = { "en", "de" }, values = { "Close", "Schließen" })
    String literally_close();

    @Default(lngs = { "en", "de" }, values = { "unknown", "unbekannt" })
    String literally_unknown();

    @Default(lngs = { "en", "de" }, values = { "Estimated remaining wait time: %s1", "Erwartete Wartezeit: %s1" })
    String installframe_statusmsg_preparing_eta(String formatSeconds);

    @Default(lngs = { "en", "de" }, values = { "There is not enough free space on your harddisk C:.\r\nMake sure that there are at least %s1 of free space (%s2 more needed) and restart the update.", "Es ist nicht genügend Speicherplatz auf C:\\ verfügbar. \r\nBitte stellen Sie sicher dass %s1 frei sind (%s2 müssen gelöscht werden) und starten Sie das Update erneut." })
    String errordialog_not_enough_space(String formatBytes, String string);

    @Default(lngs = { "en", "de" }, values = { "Insufficient permissions to install %s1.\r\nPlease contact your support!", "Fehlende Schreibrechte zur Installation von %s1.\r\nBitte kontaktieren Sie unseren Support." })
    String errordialog_cannot_write(String localizedMessage);

    @Default(lngs = { "en", "de" }, values = { "Missing write permission to install the update.\r\nPlease contact your support!", "Fehlende Schreibrechte zur Installation des Updates.\r\nBitte kontaktieren Sie unseren Support." })
    String errordialog_cannot_write2();

    @Default(lngs = { "en", "de" }, values = { "Cancel", "Abbrechen" })
    String literally_cancel();

    @Default(lngs = { "en", "de" }, values = { "Extracting Updatepackage - please wait...", "Das Updatepaket wird entpackt. Bitte warten Sie..." })
    String installframe_statusmsg_extracting();

    @Default(lngs = { "en", "de" }, values = { "A new Update is available. Do you want to download it now?\r\nUpdate size: %s1", "Ein neues Update steht zum Download bereit. Soll es jetzt geladen werden?\r\nGröße des Updates: %s1\r\n" })
    String confirmdialog_new_update_available_message_sized(String formatBytes);

    @Default(lngs = { "en", "de" }, values = { "An Update is ready for installation. We recommend to install it now!\r\nDo you want to install it now?", "Ein Update wartet darauf installiert zu werden. Wir empfehlen das jetzt zu tun!\r\nSoll das Update jetzt installiert werden?" })
    String confirmdialog_new_update_available_for_install_message_launcher();

    @Default(lngs = { "en", "de" }, values = { "Manage Extensions", "Erweiterungen verwalten" })
    String confirmdialog_new_update_available_frametitle_extensions();

    @Default(lngs = { "en", "de" }, values = { "Add %s1 and remove %s2 JDownloader extension(s)?. We recommend to do this now!\r\nDo you want to continue now?", "%s1 JDownloader Erweiterungen hinzufügen und %s2 entfernen?. Wir empfehlen das jetzt zu tun!\r\nJetzt fortfahren?" })
    String confirmdialog_new_update_available_for_install_message_extensions(int install, int uninstall);

    @Default(lngs = { "en", "de" }, values = { "An unexpected IO error occured during the update.\r\nPlease try again or contact our support.", "Während dem Update ist ein IO Fehler aufgetreten.\r\nVersuchen Sie es erneut, oder kontaktieren Sie unseren Support." })
    String errordialog_defaultio();

    @Default(lngs = { "en", "de" }, values = { "Failed to install the update. Please try again later.\r\nIf this problem does not \"solve itself\" after a few hours, contact our support or reinstall JDownloader.", "Das Update konnte nicht installiert werden. Bitte versuch es später erneut.\r\nInstalliere JDownloader bitte neu oder kontaktiere unseren Support falls sich das Problem nicht innerhalb einiger Stunden \"von selbst löst\"!" })
    String errordialog_selfupdate_failed();

    @Default(lngs = { "en", "de" }, values = { "No Internet Connection!", "Keine Internet Verbindung!" })
    String error_last_chance_connection_title();

    @Default(lngs = { "en", "de" }, values = { "JDownloader could not connect to the Internet.\r\nPlease make sure to set up your Firewall, Antivirus Software and Proxies correctly!\r\nContact our support if you cannot get rid of this problem.", "JDownloader findet keine Internet Verbindung.\r\nBitte überprüfe deine Firewall, Antiviren Software und die Proxyeinstellungen.\r\nFalls das Problem weiterhin besteht hilft unser Support gerne weiter!" })
    String error_last_chance_connection_message();

    @Default(lngs = { "en", "de" }, values = { "Update Error!", "Update Fehler!" })
    String error_last_chance_title();

    @Default(lngs = { "en", "de" }, values = { "JDownloader could not install the latest update.\r\nPlease reinstall JDownloader or contact our support!", "JDownloader kann das aktuelle Update nicht installieren.\r\nBitte installiere JDownloader neu oder kontaktiere unseren Support!" })
    String error_last_chance_message();

    @Default(lngs = { "en", "de" }, values = { "Plugins have been updated.\r\nYou're running the latest JDownloader version now.", "Plugins wurden aktualisiert.\r\nJDownloader ist nun wieder aktuell." })
    String updatedplugins();

    @Default(lngs = { "en", "de" }, values = { "A new JDownloader Plugin Update is available.To install, a restart is NOT required!\r\nDo you want to install the update now? ", "Ein neues JDownloader Plugin Update steht zur Installation bereit. Zur Installation muss KEIN Neustart durchgeführt werden.\r\nSoll jetzt installiert werden?" })
    String confirmdialog_new_update_available_for_install_message_plugin();

}
