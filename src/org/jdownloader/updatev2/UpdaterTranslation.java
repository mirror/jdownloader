package org.jdownloader.updatev2;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en", "de" })
public interface UpdaterTranslation extends TranslateInterface {

    ;

    @Default(lngs = { "en", "de" }, values = { "Service not available. Try again in 60 sec.)", "Service momentan nicht erreichbar. Bitte in einer Minute erneut versuchen." })
    String error_service_not_available_right_now();

    @Default(lngs = { "en", "de" }, values = { "Close", "Schließen" })
    String exit();

    @Default(lngs = { "en", "de" }, values = { "Bypass Updates. -noupdates flag is set", "Bypass Updates -noupdates Flag ist gesetzt" })
    String guiless_noupdates();

    @Default(lngs = { "en", "de" }, values = { "Ask me later!", "Später nachfragen!" })
    String install_updates_later();

    @Default(lngs = { "en", "de" }, values = { "Install now!", "Jetzt installieren!" })
    String install_updates_now();

    @Default(lngs = { "en", "de" }, values = { "Started Updater", "Updater gestartet" })
    String start();

    @Default(lngs = { "en", "de" }, values = { "Update(s) found!", "Update(s) gefunden!" })
    String udpates_found();

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

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for installation.", "%s1 Update(s) können jetzt installiert werden." })
    String updates_are_ready_for_install_now(int size);

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) are ready for installation.", "%s1 Update(s) können jetzt installiert werden." })
    String updates_ready_for_install(int size);

    @Default(lngs = { "en", "de" }, values = { "Finished Updatecheck - Exit", "Updateprüfung abgeschlossen - Exit" })
    String literally_exit();

    @Default(lngs = { "en", "de" }, values = { "Update already running", "Es läuft bereits eine Updateprüfung" })
    String update_already_running();

    @Default(lngs = { "en", "de" }, values = { "New Version found!", "Neue Version gefunden!" })
    String JDUpdater_start_updater_update_title();

    @Default(lngs = { "en", "de" }, values = { "Restart now to perform an update.", "Ein Neustart ist nötig um das Update zu beenden." })
    String JDUpdater_start_updater_update_msg();

    @Default(lngs = { "en", "de" }, values = { "Update & Restart now!", "Update & Neustart jetzt durchführen!" })
    String JDUpdater_start_restart_update_now_();

    @Default(lngs = { "en", "de" }, values = { "%s1/%s2", "%s1/%s2" })
    String progress(String formatBytes, String formatBytes2);

    @Default(lngs = { "en", "de" }, values = { "Updater", "Updater" })
    String window_title();

    @Default(lngs = { "en", "de" }, values = { "%s2 %s1-Edition does not exist any more.\r\n                  Reseted to latest stable edition.", "%s2 %s1-Edition existiert nicht mehr.\r\n                  Zurückgesetzt auf aktuelle Hauptversion." })
    String branch_resetted(String string, String string2);

    @Default(lngs = { "en", "de" }, values = { "The %s1 Edition does not exist!", "Die %s1 Edition existiert nicht!" })
    String branch_resetted2(String parameter);

    @Default(lngs = { "en", "de" }, values = { "Using %s1-Edition", "Verwende %s1-Edition" })
    String branch_updated(String string);

    @Default(lngs = { "en", "de" }, values = { "Updateserver is busy. Please wait 30 Seconds.", "Der Updateserver ist überlastet. Bitte 30 Sekunden warten." })
    String call_failed_wait();

    @Default(lngs = { "en", "de" }, values = { "Cancel update", "Update abbrechen" })
    String cancel_update();

    @Default(lngs = { "en", "de" }, values = { "Updater is outdated. Please reinstall the application.", "Das Updateprogramm ist nicht mehr aktuell. Bitte installieren Sie die Anwendung neu." })
    String cannot_selfupdate(String app);

    @Default(lngs = { "en", "de" }, values = { "Updater will update itself now.", "Updater wird sich nun selbst aktualisieren" })
    String clientUpdate();

    @Default(lngs = { "en", "de" }, values = { "Updateclient is outdated!", "Updater ist nicht aktuell!" })
    String clientupdate_title();

    @Default(lngs = { "en", "de" }, values = { "Close", "Schließen" })
    String close();

    @Default(lngs = { "en", "de" }, values = { "Could not download Update.", "Update konnte nicht heruntergeladen werden." })
    String could_not_download_file();

    @Default(lngs = { "en", "de" }, values = { "Could not install file: %s1", "Datei konnte nicht installiert werden: %s1" })
    String could_not_install_file(String absolutePath);

    @Default(lngs = { "en", "de" }, values = { "Could not overwrite file %s1.", "Datei %s1 kann nicht überschrieben werden." })
    String could_not_overwrite(String absolutePath);

    @Default(lngs = { "en", "de" }, values = { "Updater outdated. Could not update the updater!", "Updater veraltet. Konnte Updater nicht aktualisieren." })
    String could_not_update_updater();

    @Default(lngs = { "en", "de" }, values = { "Edition oudated", "Version veraltet" })
    String dialag_branch_resetted();

    @Default(lngs = { "en", "de" }, values = { "%s2 %s1-Edition does not exist any more.\r\nResetted to latest stable edition.", "%s2 %s1-Edition existiert nicht mehr.\r\nZurückgesetzt auf aktuelle Hauptversion." })
    String dialog_branch_resetted_msg(String string, String string2);

    @Default(lngs = { "en", "de" }, values = { "Really cancel update?", "Update wirklich abbrechen?" })
    String dialog_rly_cancel();

    @Default(lngs = { "en", "de" }, values = { "Download Failed. Try to resume...", "Download fehlgeschlagen. Versuche Wiederaufnahme..." })
    String download_failed_resume();

    @Default(lngs = { "en", "de" }, values = { "Download Failed. Retry...", "Download fehlgeschlagen. Neuversuch..." })
    String download_failed_retry();

    @Default(lngs = { "en", "de" }, values = { "Cannot update %s1 right now. Try again later.", "%s1 kann momentan nicht aktualisiert werden. Bitte später erneut versuchen." })
    String error_app_disabled(String appID);

    @Default(lngs = { "en", "de" }, values = { "The '%s1'-Edition cannot be updated right now. Please try again later!", "Die '%s1' Edition kann momentan nicht aktualisiert werden. Bitte später versuchen." })
    String error_invalid_branch(String name);

    @Default(lngs = { "en", "de" }, values = { "The Updateserver is busy right now. Try again later.", "Der Updateserver ist momentan überlastet. Bitte später erneut versuchen." })
    String error_locked(String appID);

    @Default(lngs = { "en", "de" }, values = { "Error occured", "Ein Fehler ist aufgetreten" })
    String error_occured();

    @Default(lngs = { "en", "de" }, values = { "Could not finish update.", "Update konnte nicht abgeschlossen werden." })
    String error_occured_detailed();

    @Default(lngs = { "en", "de" }, values = { "Update has been interrupted. Reverting changes...", "Update wurde unterbrochen. Änderungen werden rückgängig gemacht." })
    String error_occured_start_reverting();

    @Default(lngs = { "en", "de" }, values = { "%s1", "%s1" })
    String error_unknown(String string);

    @Default(lngs = { "en", "de" }, values = { "Cannot update %s1. \r\n                  Try again later!", "Kann %s1 nicht aktualisieren. \r\n                  Bitte später erneut versuchen." })
    String error_unknown_app(String appID);

    @Default(lngs = { "en", "de" }, values = { "Unknown Edition: %s1", "Unbekannte Version: %s1" })
    String error_unknown_branch(String string);

    @Default(lngs = { "en", "de" }, values = { "Unknown Updateserver problems.\r\nPlease try again later", "Unbekannte Updateprobleme.\r\n                  Bitte versuchen Sie es später erneut." })
    String error_unknown_server();

    @Default(lngs = { "en", "de" }, values = { "Uninstalled file: %s1", "Datei deinstalliert: %s1" })
    String filelog_deletedfile(String string);

    @Default(lngs = { "en", "de" }, values = { "Installed file %s1", "Datei installiert: %s1" })
    String filelog_installedfile(String absolutePath);

    @Default(lngs = { "en", "de" }, values = { "Updating %s1", "%s1 aktualisieren...." })
    String getPanelTitle(String app);

    @Default(lngs = { "en", "de" }, values = { "Update %s1 - %s2 Edition", "Aktualisiere %s1 - %s2 Edition" })
    String guiless_branch_updated(String app, String branch);

    @Default(lngs = { "en", "de" }, values = { "Please wait... preparing updatepackage", "Bitte warten... Bereite Updatepaket vor." })
    String guiless_create_package();

    @Default(lngs = { "en", "de" }, values = { "Update finished.", "Update fertig." })
    String guiless_done();

    @Default(lngs = { "en", "de" }, values = { "Downloading Updatepackage...", "Updatepaket wird heruntergeladen" })
    String guiless_download_data();

    @Default(lngs = { "en", "de" }, values = { "Project contains %s1 file(s)", "Projekt besteht aus %s1 Datei(en)" })
    String guiless_downloaded_hashlist(int size);

    @Default(lngs = { "en", "de" }, values = { "Download finished.", "Download fertiggestellt." })
    String guiless_downloadfinished();

    @Default(lngs = { "en", "de" }, values = { "Error occured. Reverting changes... ", "Fehler aufgetreten. Setze bisherige Änderungen zurück." })
    String guiless_error_occured_start_reverting();

    @Default(lngs = { "en", "de" }, values = { "Extract file(s)...", "Entpacke Datei(en)..." })
    String guiless_extract();

    @Default(lngs = { "en", "de" }, values = { "%s1 file(s) are waiting to get installed!", "%s1 Datei(en) warten auf Installation." })
    String guiless_files_ready_for_install(int size);

    @Default(lngs = { "en", "de" }, values = { "%s1 file(s) are outdated and will be removed.", "%s1 Datei(en) sind veraltet und werden entfernt." })
    String guiless_files_wait_for_removal(int size);

    @Default(lngs = { "en", "de" }, values = { "Compare filelist with installed file(s)", "Vergleiche Dateiliste mit installierten Datei(en)" })
    String guiless_filtering();

    @Default(lngs = { "en", "de" }, values = { "Installation finished", "Installation abgeschlossen" })
    String guiless_installFinished();

    @Default(lngs = { "en", "de" }, values = { "Cannot access %s2.\r\nPlease close %s1 to continue.", "Kann nicht auf %s2 zugreifen.\r\nBitte beenden Sie %s1 um die Installation zu beenden." })
    String guiless_locked_close_app(String appID, String path);

    @Default(lngs = { "en", "de" }, values = { "%s1 has been closed. Continue now.", "%s1 wurde beendet. Installation wird fortgesetzt." })
    String guiless_locked_closed_app(String appID);

    @Default(lngs = { "en", "de" }, values = { "...Progress: %s1%", "...Fortschritt: %s1%" })
    String guiless_progress(int percent);

    @Default(lngs = { "en", "de" }, values = { "Progress: %s1% | %s2/%s3", "Fortschritt: %s1% | %s2/%s3" })
    String guiless_progress2(long l, String loaded, String total);

    @Default(lngs = { "en", "de" }, values = { "Updater will update itself... ", "Updater wird sich nun selbst aktualisieren..." })
    String guiless_selfupdate();

    @Default(lngs = { "en", "de" }, values = { "Start installing %s1 file(s)", "Starte Installation von %s1 Datei(en)" })
    String guiless_start_install(int size);

    @Default(lngs = { "en", "de" }, values = { "Start Download of %s1", "Beginne Download von %s1" })
    String guiless_started_download(String formatBytes);

    @Default(lngs = { "en", "de" }, values = { "Resume Download of %s1", "Nehme Download von %s1 wieder auf" })
    String guiless_started_download_resume(String formatBytes);

    @Default(lngs = { "en", "de" }, values = { "Update failed. Cause: \r\n%s1", "Update fehlgeschlagen: Grund: \r\n%s1" })
    String guiless_update_failed(String message);

    @Default(lngs = { "en", "de" }, values = { "Update failed. Unknown Reason.", "Update fehlgeschlagen: Unbekannter Grund" })
    String guiless_update_failed_unknown();

    @Default(lngs = { "en", "de" }, values = { "%s1 file(s) must be downloaded", "%s1 Datei(en) müssen heruntergeladen werden." })
    String guiless_updates_waiting_for_download(int size);

    @Default(lngs = { "en", "de" }, values = { "User interrupted updated", "Benutzer hat Update unterbrochen." })
    String guiless_userinterrupted();

    @Default(lngs = { "en", "de" }, values = { "You already have the latest version", "Sie nutzen bereits die aktuelle Version." })
    String guiless_you_are_up2date();

    @Default(lngs = { "en", "de" }, values = { "Canceled", "Abgebrochen" })
    String interrupted_title();

    @Default(lngs = { "en", "de" }, values = { "Updating Signature Key", "Digitale Unterschrift wird erneuert" })
    String keychange();

    @Default(lngs = { "en", "de" }, values = { "- %s1", "- %s1" })
    String list_optionals(String id);

    @Default(lngs = { "en", "de" }, values = { "Optional Packages", "Zusatzpakete" })
    String list_optionals_header();

    @Default(lngs = { "en", "de" }, values = { "Cannot Update. %s1 is still running.", "Kann nicht aktualisieren. %s1 läuft noch." })
    String locked(String appID);

    @Default(lngs = { "en", "de" }, values = { "Could not install update.\r\nIt seems like %s1 is still running.\r\nPlease close %s1 main application.", "Konnte Update nicht installieren. Es scheint als würde %s1 noch laufen.\r\nBitte beenden Sie die %s1 Hauptanwendung" })
    String locked_dialog_msg(String appID);

    @Default(lngs = { "en", "de" }, values = { "Close Main Application", "Hauptanwendung beenden" })
    String locked_dialog_title();

    @Default(lngs = { "en", "de" }, values = { "Found: %s2 file(s) to download, %s1 file(s) to install & %s3 oudated file(s)", "Gefunden: %s2 Datei(en) herunterladen, %s1 Datei(en) installieren & %s3 alte Datei(en) entfernen." })
    String log_x_files_to_update_found(int uninstalled, int updates, int remove);

    @Default(lngs = { "en", "de" }, values = { "You already run the latest Version", "Sie haben bereits die neuste Version" })
    String log_you_are_up2date();

    @Default(lngs = { "en", "de" }, values = { "Please wait!", "Bitte warten!" })
    String please_wait();

    @Default(lngs = { "en", "de" }, values = { "Please wait until updater finished his job!", "Bitte warten bis der Updater fertig ist." })
    String please_wait_until_update_finished();

    @Default(lngs = { "en", "de" }, values = { "Unexpected redirection to %s1!", "Unerwartete Weiterleitung auf %s1!" })
    String redirect_error(String responseHeader);

    @Default(lngs = { "en", "de" }, values = { "Updater will restart itself now", "Updater wird sich nun selbst neu starten" })
    String restart_required_msg();

    @Default(lngs = { "en", "de" }, values = { "Resume Download Failed. Retry...", "Wiederaufnahme des Downloads fehlgeschlagen. Neuversuch..." })
    String resume_failed();

    @Default(lngs = { "en", "de" }, values = { "Update has been interrupted.", "Update wurde unterbrochen." })
    String reverting_msg();

    @Default(lngs = { "en", "de" }, values = { "Reverting...", "Zurücksetzen..." })
    String reverting_title();

    @Default(lngs = { "en", "de" }, values = { "Are you sure that you want to exit?", "Soll die Anwendung wirklich beendet werden?" })
    String rlyexit();

    @Default(lngs = { "en", "de" }, values = { "Really exit?", "Wirklich beenden?" })
    String rlyexit_title();

    @Default(lngs = { "en", "de" }, values = { "Start %s1", "%s1 starten" })
    String start_jd(String app);

    @Default(lngs = { "en", "de" }, values = { "Stop Countdown", "Countdown anhalten" })
    String stop_countdown();

    @Default(lngs = { "en", "de" }, values = { "%s1 s", "%s1 s" })
    String timer(int i);

    @Default(lngs = { "en", "de" }, values = { "%s1 Updater", "%s1 Updater" })
    String title(String appID);

    @Default(lngs = { "en", "de" }, values = { "%s1 Updater (Closes in %s2 seconds)", "%s1 Updater (Wird in %s2 s beendet)" })
    String title_timeout(String appID, int i);

    @Default(lngs = { "en", "de" }, values = { "%s1 Updater (%s1 starts in %s2 seconds)", "%s1 Updater (%s1 wird in %s2 Sekunden gestartet)" })
    String title_timeout_restart(String appID, int i);

    @Default(lngs = { "en", "de" }, values = { "Unexpected HTTP Error (%s1) - Please try again later", "Unerwarteter HTTP Fehler (%s1) - Bitte später erneut versuchen." })
    String unexpected_http_error(int code);

    @Default(lngs = { "en", "de" }, values = { "Not installed file(s): %s1", "Nicht installierte Datei(en): %s1" })
    String uninstalledfiles(int size);

    @Default(lngs = { "en", "de" }, values = { "Our Updateservers are busy right now. Please try again later.", "Unsere Updateserver sind gerade überlastet. Bitte später erneut versuchen." })
    String UpdateException_notavailable();

    @Default(lngs = { "en", "de" }, values = { "Problem Description: \r\n                  -> \"%s1\"", "Problem Beschreibung: \r\n                  -> \"%s1\"" })
    String UpdateException_notavailable2(String reason);

    @Default(lngs = { "en", "de" }, values = { "Proxy Authentication invalid: %s1", "Proxy Zugangsdaten ungültig: %s1" })
    String UpdateException_proxyauth(String message);

    @Default(lngs = { "en", "de" }, values = { "Proxy connection failed: %s1", "Proxy Verbindung fehlgeschlagen: %s1" })
    String UpdateException_proxyconnect(String message);

    @Default(lngs = { "en", "de" }, values = { "The origin of this update could not be verified!", "Die Herkunft dieses Updates konnte nicht verifiziert werden." })
    String UpdateException_SignatureViolation();

    @Default(lngs = { "en", "de" }, values = { "No Internet connection to updateserver: %s1", "Keine Internetverbindung zum Updateserver: %s1" })
    String UpdateException_socket(String message);

    @Default(lngs = { "en", "de" }, values = { "Failed updating %s1 Edition", "Aktualisieren der %s1 Edition ist fehlgeschlagen." })
    String UpdateException_unknownbranch(String branch);

    ;

    @Default(lngs = { "en", "de" }, values = { "The %s1 Edition does not exist!", "Die %s1 Edition existiert nicht!" })
    String UpdateException_unknownbranch_gui(String message);

    @Default(lngs = { "en", "de" }, values = { "Updater failed to update himself. Please try again in a few minutes.", "Updater konnte sich nicht selbst aktualisieren. Bitte versuch es in einigen Minuten nochmal." })
    String updateloop();

    @Default(lngs = { "en", "de" }, values = { "Updateloop detected!", "Updateschleife endeckt!" })
    String updateloop_title();

    @Default(lngs = { "en", "de" }, values = { "The tool to perform an update is missing.\r\nPlease download and install the latest version of %s1 from the Homepage.", "Das Programm zum aktualisieren von %s1 wurde nicht gefunden. \r\nBitte laden Sie die neuste Version von der Homepage und installieren Sie diese." })
    String Updater_missing_msg(String app);

    @Default(lngs = { "en", "de" }, values = { "Updater is missing", "Update Programm fehlt" })
    String Updater_missing_title();

    @Default(lngs = { "en", "de" }, values = { "The origin of this update could not be verified!", "Die Herkunft dieses Updates konnte nicht verifiziert werden." })
    String Updater_sign_key_mismatch();

    @Default(lngs = { "en", "de" }, values = { "Updatelog", "Update Log:" })
    String UpdateServer_UpdaterGui_layoutGUI_details();

    @Default(lngs = { "en", "de" }, values = { "An error occured: %s1", "Fehler aufgetreten: %s1" })
    String UpdateServer_UpdaterGui_onException_error_occured(String errormessage);

    @Default(lngs = { "en", "de" }, values = { "Please try again later.", "Bitte warten." })
    String UpdateServer_UpdaterGui_onServiceNotAvailable_bar();

    @Default(lngs = { "en", "de" }, values = { "Updateserver busy. Please wait or try later.", "Updateserver sind überlastet. Bitte warten." })
    String UpdateServer_UpdaterGui_onServiceNotAvailable_wait();

    @Default(lngs = { "en", "de" }, values = { "Find latest version", "Suche neuste Version" })
    String UpdateServer_UpdaterGui_onStateChange_branchlist();

    @Default(lngs = { "en", "de" }, values = { "Installing Resources", "Installiere Resourcen" })
    String UpdateServer_UpdaterGui_onStateChange_directinstall();

    @Default(lngs = { "en", "de" }, values = { "Download", "Download" })
    String UpdateServer_UpdaterGui_onStateChange_download();

    @Default(lngs = { "en", "de" }, values = { "Extract Updatepackage", "Entpacke Updatepaket" })
    String UpdateServer_UpdaterGui_onStateChange_extract();

    @Default(lngs = { "en", "de" }, values = { "Update failed", "Update fehlgeschlagen" })
    String UpdateServer_UpdaterGui_onStateChange_failed();

    @Default(lngs = { "en", "de" }, values = { "%s1", "%s1" })
    String UpdateServer_UpdaterGui_onStateChange_failed2(String message);

    @Default(lngs = { "en", "de" }, values = { "Find updates", "Suche nach Updates" })
    String UpdateServer_UpdaterGui_onStateChange_filter();

    @Default(lngs = { "en", "de" }, values = { "Contact Updateserver", "Kontaktiere Updateserver" })
    String UpdateServer_UpdaterGui_onStateChange_hashlist();

    @Default(lngs = { "en", "de" }, values = { "Installing", "Installiere Updates" })
    String UpdateServer_UpdaterGui_onStateChange_install();

    @Default(lngs = { "en", "de" }, values = { "Create Updatepackage", "Erstelle Updatepaket" })
    String UpdateServer_UpdaterGui_onStateChange_package();

    @Default(lngs = { "en", "de" }, values = { "Updater Routine will update itself now.", "Der Updateprozess wird sich nun selbst aktualisieren." })
    String UpdateServer_UpdaterGui_onStateChange_selfupdate();

    @Default(lngs = { "en", "de" }, values = { "Successful", "Erfolgreich" })
    String UpdateServer_UpdaterGui_onStateChange_successful();

    @Default(lngs = { "en", "de" }, values = { "Update %s1 | %s2-Edition", "Aktualisiere %s1 | %s2-Edition" })
    String UpdateServer_UpdaterGui_onUpdaterEvent_branch(String app, String branch);

    @Default(lngs = { "en", "de" }, values = { "Removed %s1", "Entfernt: %s1" })
    String UpdateServer_UpdaterGui_onUpdaterEvent_remove(String path);

    @Default(lngs = { "en", "de" }, values = { "%s1 update(s) found!", "%s1 Update(s) gefunden" })
    String UpdateServer_UpdaterGui_onUpdaterModuleEnd_end_filtering(int num);

    @Default(lngs = { "en", "de" }, values = { "Download %s1", "Lade %s1 herunter" })
    String UpdateServer_UpdaterGui_onUpdaterModuleStart_download(String filesize);

    @Default(lngs = { "en", "de" }, values = { "Resume Download of %s1", "Nehme Download von %s1 wieder auf" })
    String UpdateServer_UpdaterGui_onUpdaterModuleStart_download_resume(String filesize);

    @Default(lngs = { "en", "de" }, values = { "Updateserver busy.", "Updateserver überlastet." })
    String UpdateServer_UpdaterGui_runInEDT_mainbar();

    @Default(lngs = { "en", "de" }, values = { "Install path: %s1", "Installationsordner: %s1" })
    String UpdateServer_UpdaterGui_UpdaterGui_path(String path);

    @Default(lngs = { "en", "de" }, values = { "Started Update: %s1", "Updater gestarted: %s1" })
    String UpdateServer_UpdaterGui_UpdaterGui_started(String appid);

    @Default(lngs = { "en", "de" }, values = { "User interrupted Update!", "Benutzer hat Update unterbrochen!" })
    String userinterrupted();

    @Default(lngs = { "en", "de" }, values = { "No rights to write to %s1. ", "Nicht genug Rechte um nach %s1 zu schreiben." })
    String virtual_file_system_detected(String installDirFile);

    @Default(lngs = { "en", "de" }, values = { "More...", "Mehr..." })
    String details();

    @Default(lngs = { "en", "de" }, values = { "Stop Updater now. Use \r\n-install id1,id2,.. to install Packages", "Updater wurde beendet. \r\n-install id1,id2,... zum installieren von Paketen verwenden" })
    String end_list();

    @Default(lngs = { "en", "de" }, values = { "Package Failed Error. Please try again later.", "Paketfehler. Bitte später erneut versuchen." })
    String error_package_creation_failed();

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

    @Default(lngs = { "en", "de" }, values = { "Finalizing Installation...", "Installation wird abgeschlossen..." })
    String dialog_update_finalizing();

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

    @Default(lngs = { "en", "de" }, values = { "Cancel", "Abbrechen" })
    String installframe_button_cancel();

    @Default(lngs = { "en", "de" }, values = { "JDownloader Updater", "JDownloader Updater" })
    String installframe_frametitle();

    @Default(lngs = { "en", "de" }, values = { "Are you sure?", "Sind Sie sicher?" })
    String installframe_rlyclosedialog_frametitle();

    @Default(lngs = { "en", "de" }, values = { "Do you really want to interrupt the Update?", "Wollen Sie das Update wirklich unterbrechen?" })
    String installframe_rlyclosedialog_message();

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

    @Default(lngs = { "en", "de" }, values = { "No Updates available!", "Keine neuere Version gefunden!" })
    String installframe_statusmsg_noupdatesavailable();

    @Default(lngs = { "en", "de" }, values = { "Waiting for Application...", "Warte auf Anwendung..." })
    String installframe_statusmsg_portcheck();

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

    @Default(lngs = { "en", "de" }, values = { "The Update has been installed successfully.", "Das Update wurde erfolgreich installiert." })
    String installframe_statusmsg_successful();

    @Default(lngs = { "en", "de" }, values = { "Close", "Schließen" })
    String literally_close();

    @Default(lngs = { "en", "de" }, values = { "No", "Nein" })
    String literally_no();

    @Default(lngs = { "en", "de" }, values = { "unknown", "unbekannt" })
    String literally_unknown();

    @Default(lngs = { "en", "de" }, values = { "Yes", "Ja" })
    String literally_yes();

    @Default(lngs = { "en", "de" }, values = { "JDownloader", "JDownloader" })
    String tray_rightclickmenu_header();

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

    @Default(lngs = { "en", "de" }, values = { "Are you sure that you want to restart the Application?", "Soll die Anwendung wirklich neu gestartet werden?" })
    String rlyrestart();

    @Default(lngs = { "en", "de" }, values = { "Really restart?", "Wirklich neu starten?" })
    String rlyrestart_restart();

    @Default(lngs = { "en", "de" }, values = { "Please download the latest version from our Homepage and install it!", "Bitte laden Sie die neuste Version von unserer Homepage und installieren Sie diese." })
    String ensureThatUpdaterExists_msg();

    @Default(lngs = { "en", "de" }, values = { "Corrupt Installation!", "Fehlerhafte Installation!" })
    String ensureThatUpdaterExists_title();

    @Default(lngs = { "en", "de" }, values = { "Extracting Updatepackage - please wait...", "Das Updatepaket wird entpackt. Bitte warten Sie..." })
    String installframe_statusmsg_extracting();

    @Default(lngs = { "en", "de" }, values = { "A new Update is available. Do you want to download it now?\r\nUpdate size: %s1", "Ein neues Update steht zum Download bereit. Soll es jetzt geladen werden?\r\nGröße des Updates: %s1\r\n" })
    String confirmdialog_new_update_available_message_sized(String formatBytes);

    @Default(lngs = { "en", "de" }, values = { "An Update is ready for installation. We recommend to install it now!\r\nDo you want to install it now?", "Ein Update wartet darauf installiert zu werden. Wir empfehlen das jetzt zu tun!\r\nSoll das Update jetzt installiert werden?" })
    String confirmdialog_new_update_available_for_install_message_launcher();

    @Default(lngs = { "en", "de" }, values = { "Manage Extensions", "Erweiterungen verwalten" })
    String confirmdialog_new_update_available_frametitle_extensions();

    @Default(lngs = { "en", "de" }, values = { "Add %s1 and remove %s2 JDownloader extension(s)?. We recommend to do this now!\r\nDo you want to continue now?", "%s1 JDownloader Erweiterungen hinzufügen und %s2 entfernen?. Wir empfehlen das jetzt zu tun!\r\nJetzt fortfahren?" })
    String confirmdialog_new_update_available_for_install_message(int install, int uninstall);

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
