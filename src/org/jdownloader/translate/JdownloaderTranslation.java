package org.jdownloader.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface JdownloaderTranslation extends TranslateInterface {
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

    @Default(lngs = { "en", "de" }, values = { "If a Proxy Server is required to access internet, please enter proxy data here. JDownloader is able to rotate several Proxies to avoid IP waittimes. Default Proxy is used for all connections that are not IP restricted.", "Falls ein Proxy benötigt wird um ins Internet zu verbinden, kann dieser hier eingetragen werden. Um IP Wartezeit zu vermeiden, können mehrere Proxy Server eingetragen werden. Der Defaultproxy wird für alle Verbindungen ohne IP Beschränkungen verwendet." })
    String gui_settings_proxy_description();

    @Default(lngs = { "en", "de" }, values = { "Add", "Neu" })
    String basics_add();

    @Default(lngs = { "en", "de" }, values = { "Remove", "Entfernen" })
    String basics_remove();

    @Default(lngs = { "en", "de" }, values = { "Set the default download path here. Changing default path here, affects only new downloads.", "Standard Download Zielordner setzen. Eine Änderung betrifft nur neue Links." })
    String gui_settings_downloadpath_description();

    @Default(lngs = { "en", "de" }, values = { "Using the hashcheck option, JDownloader to verify your downloads for correctness after download.", "Über den automatischen Hashcheck kann JDownloader die geladenen Dateien automatisch auf Korrektheit überprüfen." })
    String gui_settings_filewriting_description();

    @Default(lngs = { "en", "de" }, values = { "Internet Connection", "Internet Verbindung" })
    String gui_settings_proxy_title();

    @Default(lngs = { "en", "de" }, values = { "Autostart Downloads?", "Downloads automatisch starten?" })
    String dialog_rly_forAutoaddAfterLinkcheck_title();

    @Default(lngs = { "en", "de" }, values = { "After adding links, JDownloader lists them in the Linkgrabber View to find file/package information likeOnlinestatus, Filesize, or Filename. Afterwards, Links are sorted into packages. Please choose whether JDownloader shall auto start download to your default downloadfolder (skip linkgabber) \"(%s1)\"afterwards, or keep links in Linkgrabber until you click [continue] manually (Use Linkgrabber). You can change this option at any time in the Linkgrabber View.",
            "JDownloader verwaltet neue Links in der \"Linksammler\" Ansicht. Hier werden Datei/Paket Informationen wie Onlinestatus, Dateigröße oder Dateiname ermittelt. Anschließend werde die Links in Pakete eingeordnet. Bitte wähle jetzt ob JDownloader nach dieser \"Dateiprüfung\" automatisch mit dem Download in deiner Standarddownloadordner(%s1) beginnt (Linksammler überspringen), oder ob die Links vorerst im Linksammler bleiben sollen, bis du auf [Weiter] klickst.(Linksammler verwenden)" })
    String dialog_rly_forAutoaddAfterLinkcheck_msg(String downloadfolder);

    @Default(lngs = { "en", "de" }, values = { "Skip Linkgrabber", "Linksammler überspringen" })
    String dialog_rly_forAutoaddAfterLinkcheck_ok();

    @Default(lngs = { "en", "de" }, values = { "Use Linkgrabber", "Linksammler verwenden" })
    String dialog_rly_forAutoaddAfterLinkcheck_cancel();

    @Default(lngs = { "en" }, values = { "Stopmark is set on Downloadlink: %s1" })
    String jd_controlling_DownloadWatchDog_stopmark_downloadlink(Object s1);

    @Default(lngs = { "en" }, values = { "Decrypt link %s1" })
    String plugins_container_decrypt(Object s1);

    @Default(lngs = { "en" }, values = { "Unexpected Error" })
    String downloadlink_status_error_unexpected();

    @Default(lngs = { "en" }, values = { "No InternetConnection?" })
    String jd_plugins_PluginForDecrypt_error_connection();

    @Default(lngs = { "en" }, values = { "Checking online availability..." })
    String gui_linkgrabber_pc_onlinecheck();

    @Default(lngs = { "en" }, values = { "Stopmark is set on Filepackage: %s1" })
    String jd_controlling_DownloadWatchDog_stopmark_filepackage(Object s1);

    @Default(lngs = { "en" }, values = { "Last finished package: Download Directory" })
    String replacer_downloaddirectory();

    @Default(lngs = { "en" }, values = { "No permissions to write to harddisk" })
    String download_error_message_iopermissions();

    @Default(lngs = { "en" }, values = { "[wait for new ip]" })
    String gui_downloadlink_hosterwaittime();

    @Default(lngs = { "en" }, values = { "Reconnect duration" })
    String gui_config_reconnect_showcase_time();

    @Default(lngs = { "en" }, values = { "%s1 Updates available" })
    String gui_mainframe_title_updatemessage2(Object s1);

    @Default(lngs = { "en" }, values = { "Outdated Javaversion found: %s1!" })
    String gui_javacheck_newerjavaavailable_title(Object s1);

    @Default(lngs = { "en" }, values = { "Showcase" })
    String gui_config_reconnect_test();

    @Default(lngs = { "en" }, values = { "Canceled Captcha Dialog" })
    String captchacontroller_cancel_dialog_allorhost();

    @Default(lngs = { "en" }, values = { "Bitte Warten...Reconnect läuft" })
    String gui_warning_reconnect_pleaseWait();

    @Default(lngs = { "en" }, values = { "File already exists." })
    String controller_status_fileexists_skip();

    @Default(lngs = { "en" }, values = { "[interrupted]" })
    String gui_downloadlink_aborted();

    @Default(lngs = { "en" }, values = { "What's new?" })
    String system_update_showchangelogv2();

    @Default(lngs = { "en" }, values = { "Account is ok" })
    String plugins_hoster_premium_status_ok();

    @Default(lngs = { "en" }, values = { "Reconnect unknown" })
    String gui_warning_reconnectunknown();

    @Default(lngs = { "en" }, values = { "Reconnect successful" })
    String gui_warning_reconnectSuccess();

    @Default(lngs = { "en" }, values = { "Plugin error. Please inform Support" })
    String plugins_errors_pluginerror();

    @Default(lngs = { "en" }, values = { "Wrong password" })
    String decrypter_wrongpassword();

    @Default(lngs = { "en" }, values = { "Last finished File: Checksum (SHA1/MD5) if set by hoster" })
    String replacer_checksum();

    @Default(lngs = { "en" }, values = { "Could not overwrite existing file" })
    String system_download_errors_couldnotoverwrite();

    @Default(lngs = { "en" }, values = { "Network problems" })
    String download_error_message_networkreset();

    @Default(lngs = { "en" }, values = { "Error" })
    String ballon_download_error_title();

    @Default(lngs = { "en" }, values = { "Waiting for user input" })
    String gui_linkgrabber_waitinguserio();

    @Default(lngs = { "en" }, values = { "<b><u>No Reconnect selected</u></b><br/><p>Reconnection is an advanced approach for skipping long waits that some hosts impose on free users. <br>It is not helpful while using a premium account.</p><p>Read more about Reconnect <a href='http://support.jdownloader.org/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=1'>here</a></p>" })
    String jd_controlling_reconnect_plugins_DummyRouterPlugin_getGUI2();

    @Default(lngs = { "en" }, values = { "<b>%s1<b><hr>File not found" })
    String ballon_download_fnf_message(Object s1);

    @Default(lngs = { "en" }, values = { "CRC-Check OK(%s1)" })
    String system_download_doCRC2_success(Object s1);

    @Default(lngs = { "en" }, values = { "unchecked" })
    String gui_linkgrabber_package_unchecked();

    @Default(lngs = { "en" }, values = { "Hoster problem?" })
    String plugins_errors_hosterproblem();

    @Default(lngs = { "en" }, values = { "Download incomplete" })
    String download_error_message_incomplete();

    @Default(lngs = { "en" }, values = { "Unexpected rangeheader format:" })
    String download_error_message_rangeheaderparseerror();

    @Default(lngs = { "en" }, values = { "JDownloader has not found anything on %s1\r\n-------------------------------\r\nJD now loads this page to look for further links." })
    String gui_dialog_deepdecrypt_message(Object s1);

    @Default(lngs = { "en" }, values = { "Last finished File: Download-URL (only for non-container links)" })
    String replacer_downloadurl();

    @Default(lngs = { "en" }, values = { "<b>%s1<b><hr>failed" })
    String ballon_download_failed_message(Object s1);

    @Default(lngs = { "en" }, values = { "Reconnect Method:" })
    String jd_controlling_reconnect_plugins_ReconnectPluginConfigGUI_initGUI_comboboxlabel();

    @Default(lngs = { "en" }, values = { "Last finished package: Password" })
    String replacer_password();

    @Default(lngs = { "en" }, values = { "Download" })
    String ballon_download_successful_title();

    @Default(lngs = { "en" }, values = { "Brought to you by" })
    String container_message_uploaded();

    @Default(lngs = { "en" }, values = { "Premium Error" })
    String downloadlink_status_error_premium();

    @Default(lngs = { "en" }, values = { "various" })
    String gui_linkgrabber_package_unsorted();

    @Default(lngs = { "en" }, values = { "[Not available]" })
    String gui_download_onlinecheckfailed();

    @Default(lngs = { "en" }, values = { "Premiumaccounts are globally disabled!<br/>Click <a href='http://jdownloader.org/knowledge/wiki/gui/premiummenu'>here</a> for help." })
    String gui_accountcontroller_globpremdisabled();

    @Default(lngs = { "en" }, values = { "No valid account found" })
    String decrypter_invalidaccount();

    @Default(lngs = { "en" }, values = { "Please enter the password for %s1" })
    String jd_plugins_PluginUtils_askPassword(Object s1);

    @Default(lngs = { "en" }, values = { "Plugin out of date" })
    String controller_status_plugindefective();

    @Default(lngs = { "en" }, values = { "Mirror %s1 is loading" })
    String system_download_errors_linkisBlocked(Object s1);

    @Default(lngs = { "en" }, values = { "Stopmark is still set!" })
    String jd_controlling_DownloadWatchDog_stopmark_set();

    @Default(lngs = { "en" }, values = { "Download failed" })
    String downloadlink_status_error_downloadfailed();

    @Default(lngs = { "en" }, values = { "Unknown error, retrying" })
    String downloadlink_status_error_retry();

    @Default(lngs = { "en" }, values = { "Reconnect failed!" })
    String gui_warning_reconnectFailed();

    @Default(lngs = { "en" }, values = { "running..." })
    String gui_warning_reconnect_running();

    @Default(lngs = { "en" }, values = { "Last finished File: Hoster" })
    String replacer_hoster();

    @Default(lngs = { "en" }, values = { "Password" })
    String container_message_password();

    @Default(lngs = { "en" }, values = { "Skip Link" })
    String system_download_triggerfileexists_skip();

    @Default(lngs = { "en" }, values = { "Open Container" })
    String plugins_container_open();

    @Default(lngs = { "en" }, values = { "Your current IP" })
    String gui_config_reconnect_showcase_currentip();

    @Default(lngs = { "en" }, values = { "Last finished package: Filelist" })
    String replacer_filelist();

    @Default(lngs = { "en" }, values = { "CRC-Check FAILED(%s1)" })
    String system_download_doCRC2_failed(Object s1);

    @Default(lngs = { "en" }, values = { "No Internet connection?" })
    String plugins_errors_nointernetconn();

    @Default(lngs = { "en" }, values = { "Update successful" })
    String system_update_message();

    @Default(lngs = { "en" }, values = { "Processing error" })
    String downloadlink_status_error_post_process();

    @Default(lngs = { "en" }, values = { "Download from this host is currently not possible" })
    String downloadlink_status_error_hoster_temp_unavailable();

    @Default(lngs = { "en" }, values = { "To allow JDownloader to perform automated reconnections, you should enable this feature!" })
    String gui_warning_reconnect_hasbeendisabled_tooltip();

    @Default(lngs = { "en" }, values = { "Disconnect?" })
    String plugins_errors_disconnect();

    @Default(lngs = { "en" }, values = { "DownloadLinkContainer loaded" })
    String container_message_title();

    @Default(lngs = { "en" }, values = { "AbstractExtension" })
    String D_User_thomas_workspacejd_JDownloader_src_org_jdownloader_extensions_AbstractExtension();

    @Default(lngs = { "en" }, values = { "CRC-Check running(%s1)" })
    String system_download_doCRC2(Object s1);

    @Default(lngs = { "en" }, values = { "File exists" })
    String jd_controlling_SingleDownloadController_askexists_title();

    @Default(lngs = { "en" }, values = { "[download currently not possible]" })
    String gui_downloadlink_hostertempunavail();

    @Default(lngs = { "en" }, values = { "Invalid Outputfile" })
    String system_download_errors_invalidoutputfile();

    @Default(lngs = { "en" }, values = { "Last finished File: is Available (Yes,No)" })
    String replacer_available();

    @Default(lngs = { "en" }, values = { "Click'n'Load" })
    String jd_controlling_CNL2_checkText_title();

    @Default(lngs = { "en" }, values = { "Last finished File: Filename" })
    String replacer_filename();

    @Default(lngs = { "en" }, values = { "Could not clone the connection" })
    String download_error_message_connectioncopyerror();

    @Default(lngs = { "en" }, values = { "Wait %s1" })
    String gui_download_waittime_status2(Object s1);

    @Default(lngs = { "en" }, values = { "You canceled a Captcha Dialog!\r\nHow do you want to continue?" })
    String captchacontroller_cancel_dialog_allorhost_msg();

    @Default(lngs = { "en" }, values = { "Stopping all downloads %s1" })
    String jd_controlling_DownloadWatchDog_stopping(Object s1);

    @Default(lngs = { "en" }, values = { "Wrong captcha code" })
    String decrypter_wrongcaptcha();

    @Default(lngs = { "en" }, values = { "Server does not support chunkload" })
    String download_error_message_rangeheaders();

    @Default(lngs = { "en" }, values = { "Parse %s1 URL(s). Found %s2 links" })
    String gui_addurls_progress_found(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "Active" })
    String gui_treetable_packagestatus_links_active();

    @Default(lngs = { "en" }, values = { "Reconnect failed! Please check your reconnect Settings and try a Manual Reconnect!" })
    String jd_controlling_reconnect_Reconnector_progress_failed();

    @Default(lngs = { "en" }, values = { "Captcha recognition" })
    String gui_downloadview_statustext_jac();

    @Default(lngs = { "en" }, values = { "Show all further pending Captchas" })
    String captchacontroller_cancel_dialog_allorhost_next();

    @Default(lngs = { "en" }, values = { "Error. User aborted installation." })
    String installer_abortInstallation();

    @Default(lngs = { "en" }, values = { "Although JDownloader runs on your javaversion, we advise to install the latest java updates. \r\nJDownloader will run more stable, faster, and will look better. \r\n\r\nVisit http://jdownloader.org/download." })
    String gui_javacheck_newerjavaavailable_msg();

    @Default(lngs = { "en" }, values = { "No Connection" })
    String downloadlink_status_error_no_connection();

    @Default(lngs = { "en" }, values = { "Auto rename" })
    String system_download_triggerfileexists_rename();

    @Default(lngs = { "en" }, values = { "Download Limit reached" })
    String downloadlink_status_error_download_limit();

    @Default(lngs = { "en" }, values = { "Password wrong: %s1" })
    String jd_plugins_PluginUtils_informPasswordWrong_title(Object s1);

    @Default(lngs = { "en" }, values = { "File exists" })
    String downloadlink_status_error_file_exists();

    @Default(lngs = { "en" }, values = { "Reconnect failed too often! Autoreconnect is disabled! Please check your reconnect Settings!" })
    String jd_controlling_reconnect_Reconnector_progress_failed2();

    @Default(lngs = { "en" }, values = { "The file \r\n%s1\r\n already exists. What do you want to do?" })
    String jd_controlling_SingleDownloadController_askexists(Object s1);

    @Default(lngs = { "en" }, values = { "Captcha wrong" })
    String downloadlink_status_error_captcha_wrong();

    @Default(lngs = { "en" }, values = { "jDownloader: Revision/Version" })
    String replacer_jdversion();

    @Default(lngs = { "en" }, values = { "Last finished package: Packagename" })
    String replacer_packagename();

    @Default(lngs = { "en" }, values = { "ETA" })
    String gui_eta();

    @Default(lngs = { "en" }, values = { "offline" })
    String gui_linkgrabber_package_offline();

    @Default(lngs = { "en" }, values = { "Not tested yet" })
    String gui_config_reconnect_showcase_message_none();

    @Default(lngs = { "en" }, values = { "Do not show pending Captchas for %s1" })
    String captchacontroller_cancel_dialog_allorhost_cancelhost(Object s1);

    @Default(lngs = { "en" }, values = { "Last finished package: Comment" })
    String replacer_comment();

    @Default(lngs = { "en" }, values = { "Accountmanager" })
    String gui_ballon_accountmanager_title();

    @Default(lngs = { "en" }, values = { "The password you entered for %s1 has been wrong." })
    String jd_plugins_PluginUtils_informPasswordWrong_message(Object s1);

    @Default(lngs = { "en" }, values = { "<b>%s1<b><hr>finished successfully" })
    String ballon_download_successful_message(Object s1);

    @Default(lngs = { "en" }, values = { "Unknown error" })
    String decrypter_unknownerror();

    @Default(lngs = { "en" }, values = { "Reconnect #" })
    String jd_controlling_reconnect_plugins_ReconnectPluginController_doReconnect_1();

    @Default(lngs = { "en" }, values = { "Container error: %s1" })
    String plugins_container_exit_error(Object s1);

    @Default(lngs = { "en" }, values = { "Comment" })
    String container_message_comment();

    @Default(lngs = { "en" }, values = { "Current Time" })
    String replacer_time();

    @Default(lngs = { "en" }, values = { "Error. You do not have permissions to write to the dir" })
    String installer_error_noWriteRights();

    @Default(lngs = { "en" }, values = { "Used Java Version" })
    String replacer_javaversion();

    @Default(lngs = { "en" }, values = { "Temporarily unavailable" })
    String controller_status_tempunavailable();

    @Default(lngs = { "en" }, values = { "Updated to version %s1" })
    String system_update_message_title(Object s1);

    @Default(lngs = { "en" }, values = { "Last finished package: Auto Password" })
    String replacer_autopassword();

    @Default(lngs = { "en" }, values = { "Cancel all pending Captchas" })
    String captchacontroller_cancel_dialog_allorhost_all();

    @Default(lngs = { "en" }, values = { "jDownloader: Homedirectory/Installdirectory" })
    String replacer_jdhomedirectory();

    @Default(lngs = { "en" }, values = { "Prozess %s1 links" })
    String plugins_container_found(Object s1);

    @Default(lngs = { "en" }, values = { "DLC encryption successful. Run Testdecrypt now?" })
    String sys_dlc_success();

    @Default(lngs = { "en" }, values = { "Deep decryption?" })
    String gui_dialog_deepdecrypt_title();

    @Default(lngs = { "en" }, values = { "<b>%s1<b><hr>Plugin defect" })
    String ballon_download_plugindefect_message(Object s1);

    @Default(lngs = { "en" }, values = { "Download" })
    String download_connection_normal();

    @Default(lngs = { "en" }, values = { "<b>%s1<b><hr>Connection lost" })
    String ballon_download_connectionlost_message(Object s1);

    @Default(lngs = { "en" }, values = { "Overwrite" })
    String system_download_triggerfileexists_overwrite();

    @Default(lngs = { "en" }, values = { "Continue" })
    String gui_btn_continue();

    @Default(lngs = { "en" }, values = { "Ip before reconnect" })
    String gui_config_reconnect_showcase_lastip();

    @Default(lngs = { "en" }, values = { "Hoster offline?" })
    String plugins_errors_hosteroffline();

    @Default(lngs = { "en" }, values = { "Decrypt %s1: %s2" })
    String jd_plugins_PluginForDecrypt_decrypting(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "[convert failed]" })
    String gui_downloadlink_errorpostprocess3();

    @Default(lngs = { "en" }, values = { "Start Test" })
    String gui_config_reconnect_showcase_reconnect2();

    @Default(lngs = { "en" }, values = { "Connecting..." })
    String gui_download_create_connection();

    @Default(lngs = { "en" }, values = { "Container Error" })
    String controller_status_containererror();

    @Default(lngs = { "en" }, values = { "dd.MM.yy HH:mm" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_TableRenderer_dateformat();

    @Default(lngs = { "en" }, values = { "No plugin available" })
    String downloadlink_status_error_no_plugin_available();

    @Default(lngs = { "en" }, values = { "Reconnection has been disabled!" })
    String gui_warning_reconnect_hasbeendisabled();

    @Default(lngs = { "en" }, values = { "Connection lost." })
    String controller_status_connectionproblems();

    @Default(lngs = { "en" }, values = { "The downloadsystem is out of memory" })
    String download_error_message_outofmemory();

    @Default(lngs = { "en" }, values = { "Plugin outdated" })
    String downloadlink_status_error_defect();

    @Default(lngs = { "en" }, values = { "Failed to overwrite" })
    String controller_status_fileexists_overwritefailed();

    @Default(lngs = { "en" }, values = { "Parse %s1 URL(s)" })
    String gui_addurls_progress(Object s1);

    @Default(lngs = { "en" }, values = { "Service temp. unavailable" })
    String download_error_message_unavailable();

    @Default(lngs = { "en" }, values = { "Waiting for user input: %s1" })
    String gui_linkgrabber_waitinguserio2(Object s1);

    @Default(lngs = { "en" }, values = { "Serverproblem?" })
    String jd_plugins_PluginForDecrypt_error_server();

    @Default(lngs = { "en" }, values = { "Aborted" })
    String gui_linkgrabber_aborted();

    @Default(lngs = { "en" }, values = { "Stop after current Downloads" })
    String jd_gui_swing_jdgui_actions_actioncontroller_toolbar_control_stopmark_tooltip();

    @Default(lngs = { "en" }, values = { "Could not write to file: %s1" })
    String download_error_message_localio(Object s1);

    @Default(lngs = { "en" }, values = { "Plugin has no handlePremium Method!" })
    String plugins_hoster_nopremiumsupport();

    @Default(lngs = { "en" }, values = { "Loading from" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_loadingFrom();

    @Default(lngs = { "en" }, values = { "Not enough harddiskspace" })
    String downloadlink_status_error();

    @Default(lngs = { "en" }, values = { "Temp. unavailable" })
    String downloadlink_status_error_temp_unavailable();

    @Default(lngs = { "en" }, values = { "Error: " })
    String plugins_errors_error();

    @Default(lngs = { "en" }, values = { "Could not rename partfile" })
    String system_download_errors_couldnotrename();

    @Default(lngs = { "en" }, values = { "Finished. Found %s1 links" })
    String plugins_container_exit(Object s1);

    @Default(lngs = { "en" }, values = { "(Filesize unknown)" })
    String gui_download_filesize_unknown();

    @Default(lngs = { "en" }, values = { "Created with" })
    String container_message_created();

    @Default(lngs = { "en" }, values = { "Current IP Address" })
    String replacer_ipaddress();

    @Default(lngs = { "en" }, values = { "filtered" })
    String gui_linkgrabber_package_filtered();

    @Default(lngs = { "en" }, values = { "Waiting for user input" })
    String downloadlink_status_waitinguserio();

    @Default(lngs = { "en" }, values = { "File loaded from %s1." })
    String controller_status_fileexists_othersource(Object s1);

    @Default(lngs = { "en" }, values = { "Password wrong" })
    String plugins_errors_wrongpassword();

    @Default(lngs = { "en" }, values = { "Decrypter out of date: %s1" })
    String jd_plugins_PluginForDecrypt_error_outOfDate(Object s1);

    @Default(lngs = { "en" }, values = { "<b>%s1<b><hr>Fatal Plugin Error" })
    String ballon_download_fatalerror_message(Object s1);

    @Default(lngs = { "en" }, values = { "Current Date" })
    String replacer_date();

    @Default(lngs = { "en" }, values = { "File not found" })
    String downloadlink_status_error_file_not_found();

    @Default(lngs = { "en" }, values = { "various" })
    String controller_packages_defaultname();

    @Default(lngs = { "en" }, values = { "Autostart downloads in few seconds..." })
    String gui_autostart();

    @Default(lngs = { "en" }, values = { "Last finished File: Browser-URL" })
    String replacer_browserurl();

    @Default(lngs = { "en" }, values = { "Last finished File: Filesize" })
    String replacer_filesize();

    @Default(lngs = { "en" }, values = { "Account" })
    String jd_plugins_PluginsForHost_account();

    @Default(lngs = { "en" }, values = { "Last finished File: Filepath" })
    String replacer_filepath();

    @Default(lngs = { "en" }, values = { "No Reconnect" })
    String jd_controlling_reconnect_plugins_DummyRouterPlugin_getName();

    @Default(lngs = { "en" }, values = { "Click'n'Load URL opened" })
    String jd_controlling_CNL2_checkText_message();

    @Default(lngs = { "en" }, values = { "Parse %s1 URL(s). Get %s2 links" })
    String gui_addurls_progress_get(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "Download failed" })
    String plugins_error_downloadfailed();

    @Default(lngs = { "en" }, values = { "Fatal Error" })
    String downloadlink_status_error_fatal();

    @Default(lngs = { "en" }, values = { "Incomplete" })
    String downloadlink_status_incomplete();

    @Default(lngs = { "en" }, values = { "Download Managment" })
    String gui_settings_downloadcontroll_title();

    @Default(lngs = { "en" }, values = { "Connection limits, Download order, Priorities, .... set up the Downloadcontroller details." })
    String gui_settings_downloadcontroll_description();

    @Default(lngs = { "en" }, values = { "Link Grabber" })
    String gui_settings_linkgrabber_title();

    @Default(lngs = { "en" }, values = { "The \"Link Grabber\" is used to find mirrors, check link stati, and to order your downloads into packages before downloading." })
    String gui_settings_linkgrabber_description();

    @Default(lngs = { "en" }, values = { "Set up filters, to ignore files based on their address or filename" })
    String gui_settings_linkgrabber_filter_description();

    @Default(lngs = { "en" }, values = { "Accessibility" })
    String gui_settings_barrierfree_title();

    @Default(lngs = { "en" }, values = { "All options here help to make JDownloader usable by people of all abilities and disabilities." })
    String gui_settings_barrierfree_description();

    @Default(lngs = { "en" }, values = { "Credentials" })
    String gui_settings_logins_title();

    @Default(lngs = { "en" }, values = { "HTTP Credentials" })
    String gui_settings_logins_htaccess();

    @Default(lngs = { "en" }, values = { "FTP Credentials" })
    String gui_settings_logins_ftp();

    @Default(lngs = { "en" }, values = { "Account Manager" })
    String gui_settings_premium_title();

    @Default(lngs = { "en" }, values = { "Enter and manage all your Premium/Gold/Platin accounts." })
    String gui_settings_premium_description();

    @Default(lngs = { "en" }, values = { "Account %s1@%s2" })
    String pluginforhost_infogenerator_title(String user, String hoster);

    @Default(lngs = { "en" }, values = { "Basic Authentication" })
    String gui_settings_basicauth_title();

    @Default(lngs = { "en" }, values = { "Add HTTP and FTP credentials here. Basic Authentication can be used for basic logins which do not need an extra Plugin.\r\n\r\nUse the Account Manager for Premium/Gold/Platin Accounts!" })
    String gui_settings_basicauth_description();

    @Default(lngs = { "en" }, values = { "Reconnect" })
    String gui_settings_reconnect_title();

    @Default(lngs = { "en" }, values = { "Reconnection helps you to avoid waittimes. Click [here] for details" })
    String gui_settings_reconnect_description();

    @Default(lngs = { "en" }, values = { "http://support.jdownloader.org/index.php?_m=knowledgebase&_a=viewarticle&kbarticleid=1" })
    String gui_settings_reconnect_description_url();

    @Default(lngs = { "en" }, values = { "Reconnect Wizard" })
    String reconnectmanager_wizard();

    @Default(lngs = { "en" }, values = { "Reconnect Method" })
    String gui_settings_reconnect_title_method();

    @Default(lngs = { "en" }, values = { "Test Settings" })
    String gui_settings_reconnect_title_test();

    @Default(lngs = { "en" }, values = { "Extension Modules" })
    String gui_settings_extensions_description();

    @Default(lngs = { "en" }, values = { "Enabled/Disable this Extension" })
    String settings_sidebar_tooltip_enable_extension();

    @Default(lngs = { "en" }, values = { "Enabled" })
    String configheader_enabled();

    @Default(lngs = { "en" }, values = { "Plugin Settings" })
    String gui_settings_plugins_title();

    @Default(lngs = { "en" }, values = { "JDownloader uses 'Plugins' for %s1 websites to automate downloads which would take a lot of time without JDownloader. Some of these Plugins have settings to customize their behaviour." })
    String gui_settings_plugins_description(int num);

    @Default(lngs = { "en" }, values = { "All Settings found here are for Advanced Users only! Do not change anything here if you do not know 100% what you are doing." })
    String gui_settings_advanced_description();
}