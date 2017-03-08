package org.jdownloader.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.DescriptionForTranslationEntry;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface JdownloaderTranslation extends TranslateInterface {
    //
    @Default(lngs = { "en", "de" }, values = { "Error occured!", "Fehler aufgetreten!" })
    String dialog_title_exception();

    @Default(lngs = { "en", "de" }, values = { "Tools", "Tools" })
    String gui_menu_extensions();

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

    @Default(lngs = { "en", "de" }, values = { "Choose directory", "Ordner auswählen" })
    String gui_setting_folderchooser_title();

    @Default(lngs = { "en", "de" }, values = { "If a Proxy Server is required to access internet, please enter proxy data here. JDownloader is able to rotate several Proxies to avoid IP wait times. Default Proxy is used for all connections that are not IP restricted.", "Falls ein Proxy benötigt wird um ins Internet zu verbinden, kann dieser hier eingetragen werden. Um IP Wartezeit zu vermeiden, können mehrere Proxy Server eingetragen werden. Der Defaultproxy wird für alle Verbindungen ohne IP Beschränkungen verwendet." })
    String gui_settings_proxy_description();

    @Default(lngs = { "en", "de" }, values = { "Set the default download path here. Changing default path here, affects only new downloads.", "Standard Download Zielordner setzen. Eine Änderung betrifft nur neue Links." })
    String gui_settings_downloadpath_description();

    @Default(lngs = { "en", "de" }, values = { "Using the hashcheck option, JDownloader to verify your downloads for correctness after download.", "Über den automatischen Hashcheck kann JDownloader die geladenen Dateien automatisch auf Korrektheit überprüfen." })
    String gui_settings_filewriting_description();

    @Default(lngs = { "en", "de" }, values = { "Connection Manager", "Verbindungsverwaltung" })
    String gui_settings_proxy_title();

    @Default(lngs = { "en" }, values = { "No permissions to write to harddisk" })
    String download_error_message_iopermissions();

    @Default(lngs = { "en" }, values = { "Reconnect duration" })
    String gui_config_reconnect_showcase_time();

    @Default(lngs = { "en" }, values = { "%s1 Updates available" })
    String gui_mainframe_title_updatemessage2(Object s1);

    @Default(lngs = { "en" }, values = { "Outdated Javaversion found: %s1!" })
    String gui_javacheck_newerjavaavailable_title(Object s1);

    @Default(lngs = { "en" }, values = { "Reconnect unknown" })
    String gui_warning_reconnectunknown();

    @Default(lngs = { "en" }, values = { "Reconnect successful" })
    String gui_warning_reconnectSuccess();

    @Default(lngs = { "en" }, values = { "Wrong password" })
    String decrypter_wrongpassword();

    @Default(lngs = { "en" }, values = { "Plugin Defect" })
    String decrypter_plugindefect();

    @Default(lngs = { "en" }, values = { "Could not delete existing part file" })
    String system_download_errors_couldnotdelete();

    @Default(lngs = { "en" }, values = { "Network problems" })
    String download_error_message_networkreset();

    @Default(lngs = { "en" }, values = { "<b><u>No Reconnect selected</u></b><br/><p>Reconnection is an advanced approach for skipping long waits that some hosts impose on free users. <br>It is not helpful while using a premium account.</p><p>Read more about Reconnect <a href='http://support.jdownloader.org/index.php?/Knowledgebase/Article/View/1/0/why-should-i-set-up-my-reconnect'>here</a></p>" })
    String jd_controlling_reconnect_plugins_DummyRouterPlugin_getGUI2();

    @Default(lngs = { "en" }, values = { "[%s1] CRC OK" })
    String system_download_doCRC2_success(Object s1);

    @Default(lngs = { "en" }, values = { "Hoster problem?" })
    String plugins_errors_hosterproblem();

    @Default(lngs = { "en" }, values = { "Download incomplete" })
    String download_error_message_incomplete();

    @Default(lngs = { "en" }, values = { "Unexpected rangeheader format:" })
    String download_error_message_rangeheaderparseerror();

    @Default(lngs = { "en" }, values = { "No valid account found" })
    String decrypter_invalidaccount();

    @Default(lngs = { "en" }, values = { "Please enter the password for\r\n%s1" })
    String jd_plugins_PluginUtils_askPassword(Object s1);

    @Default(lngs = { "en" }, values = { "Mirror %s1 is loading" })
    String system_download_errors_linkisBlocked(Object s1);

    @Default(lngs = { "en" }, values = { "Download failed" })
    String downloadlink_status_error_downloadfailed();

    @Default(lngs = { "en" }, values = { "Reconnect failed!" })
    String gui_warning_reconnectFailed();

    @Default(lngs = { "en" }, values = { "running..." })
    String gui_warning_reconnect_running();

    @Default(lngs = { "en" }, values = { "Your current IP" })
    String gui_config_reconnect_showcase_currentip();

    @Default(lngs = { "en" }, values = { "[%s1] CRC FAILED" })
    String system_download_doCRC2_failed(Object s1);

    @Default(lngs = { "en" }, values = { "No Internet connection?" })
    String plugins_errors_nointernetconn();

    @Default(lngs = { "en" }, values = { "Download from this host is currently not possible" })
    String downloadlink_status_error_hoster_temp_unavailable();

    @Default(lngs = { "en" }, values = { "Disconnect?" })
    String plugins_errors_disconnect();

    @Default(lngs = { "en" }, values = { "[%s1] CRC running" })
    String system_download_doCRC2(Object s1);

    @Default(lngs = { "en" }, values = { "Waiting for Hashcheck" })
    String system_download_doCRC2_waiting();

    @Default(lngs = { "en" }, values = { "File exists" })
    String jd_controlling_SingleDownloadController_askexists_title();

    @Default(lngs = { "en" }, values = { "Could not clone the connection" })
    String download_error_message_connectioncopyerror();

    @Default(lngs = { "en" }, values = { "Wait %s1" })
    String gui_download_waittime_status2(Object s1);

    @Default(lngs = { "en" }, values = { "Not enough traffic available (%s1 required)" })
    String gui_download_waittime_notenoughtraffic(String trafficRequired);

    @Default(lngs = { "en" }, values = { "Not enough traffic available" })
    String gui_download_waittime_notenoughtraffic2();

    @Default(lngs = { "en" }, values = { "Wrong captcha code" })
    String decrypter_wrongcaptcha();

    @Default(lngs = { "en" }, values = { "Server does not support chunkload" })
    String download_error_message_rangeheaders();

    @Default(lngs = { "en" }, values = { "Captcha recognition" })
    String gui_downloadview_statustext_jac();

    @Default(lngs = { "en" }, values = { "Although JDownloader runs on your javaversion, we advise to install the latest java updates. \r\nJDownloader will run more stable, faster, and will look better. \r\n\r\nVisit http://jdownloader.org/download." })
    String gui_javacheck_newerjavaavailable_msg();

    @Default(lngs = { "en" }, values = { "Download Limit reached" })
    String downloadlink_status_error_download_limit();

    @Default(lngs = { "en" }, values = { "File exists" })
    String downloadlink_status_error_file_exists();

    @Default(lngs = { "en" }, values = { "Reconnect failed too often! Autoreconnect is disabled! Please check your reconnect Settings!" })
    String jd_controlling_reconnect_Reconnector_progress_failed2();

    @Default(lngs = { "en" }, values = { "There is a problem downloading a file!\r\nThe file already exists on harddisk. What do you want to do?" })
    String jd_controlling_SingleDownloadController_askexists3();

    @Default(lngs = { "en" }, values = { "Not tested yet" })
    String gui_config_reconnect_showcase_message_none();

    @Default(lngs = { "en" }, values = { "Unknown error" })
    String decrypter_unknownerror();

    @Default(lngs = { "en" }, values = { "Download" })
    String download_connection_normal();

    @Default(lngs = { "en" }, values = { "Ip before reconnect" })
    String gui_config_reconnect_showcase_lastip();

    @Default(lngs = { "en" }, values = { "Hoster offline?" })
    String plugins_errors_hosteroffline();

    @Default(lngs = { "en" }, values = { "Start Test" })
    String gui_config_reconnect_showcase_reconnect2();

    @Default(lngs = { "en" }, values = { "The downloadsystem is out of memory" })
    String download_error_message_outofmemory();

    @Default(lngs = { "en" }, values = { "Plugin outdated" })
    String downloadlink_status_error_defect();

    @Default(lngs = { "en" }, values = { "Service temp. unavailable" })
    String download_error_message_unavailable();

    @Default(lngs = { "en" }, values = { "Could not write to file: %s1" })
    String download_error_message_localio(Object s1);

    @Default(lngs = { "en" }, values = { "Temp. unavailable" })
    String downloadlink_status_error_temp_unavailable();

    @Default(lngs = { "en" }, values = { "Error: " })
    String plugins_errors_error();

    @Default(lngs = { "en" }, values = { "Could not rename partfile" })
    String system_download_errors_couldnotrename();

    @Default(lngs = { "en" }, values = { "(Filesize unknown)" })
    String gui_download_filesize_unknown();

    @Default(lngs = { "en" }, values = { "Password wrong" })
    String plugins_errors_wrongpassword();

    @Default(lngs = { "en" }, values = { "File not found" })
    String downloadlink_status_error_file_not_found();

    @Default(lngs = { "en" }, values = { "various" })
    String controller_packages_defaultname();

    @Default(lngs = { "en" }, values = { "No Reconnect" })
    String jd_controlling_reconnect_plugins_DummyRouterPlugin_getName();

    @Default(lngs = { "en" }, values = { "Fatal Error" })
    String downloadlink_status_error_fatal();

    @Default(lngs = { "en" }, values = { "Download Managment" })
    String gui_settings_downloadcontroll_title();

    @Default(lngs = { "en" }, values = { "Connection limits, Download order, Priorities, .... set up the Downloadcontroller details." })
    String gui_settings_downloadcontroll_description();

    @Default(lngs = { "en" }, values = { "Linkgrabber Filter" })
    String gui_settings_linkgrabber_title();

    @Default(lngs = { "en" }, values = { "The linkfilter is used to filter or group links. Use it to ignore or group links, adresses, urls or files based on their properties. Set up 'Views' for the Linkgrabber's sidebar, or 'Filters' to avoid grabbing special links completely..." })
    String gui_settings_linkgrabber_filter_description2();

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

    @Default(lngs = { "en" }, values = { "Reconnect Wizard" })
    String reconnectmanager_wizard();

    @Default(lngs = { "en" }, values = { "Reconnect Method" })
    String gui_settings_reconnect_title_method();

    @Default(lngs = { "en" }, values = { "Enabled/Disable this Extension" })
    String settings_sidebar_tooltip_enable_extension();

    @Default(lngs = { "en" }, values = { "Enabled" })
    String configheader_enabled();

    @Default(lngs = { "en" }, values = { "JDownloader uses 'Plugins' for %s1 websites to automate downloads which would take a lot of time without JDownloader. Some of these Plugins have settings to customize their behaviour." })
    String gui_settings_plugins_description(int num);

    @Default(lngs = { "en" }, values = { "All Settings found here are for Advanced Users only! Do not change anything here if you do not know 100% what you are doing." })
    String gui_settings_advanced_description();

    @Default(lngs = { "en" }, values = { "Offline" })
    String literally_offline();

    @Default(lngs = { "en" }, values = { "The Packagizer rules let you auto-set Download Settings on Files based on their properties." })
    String gui_settings_linkgrabber_packagizer_description();

    @Default(lngs = { "en" }, values = { "%s1 (copy)" })
    String LinkgrabberFilterRule_duplicate(String name);

    @Default(lngs = { "en" }, values = { "Duplicate Rule" })
    String DuplicateAction_DuplicateAction_();

    @Default(lngs = { "en" }, values = { "Save rules to a file.\r\n- Backup\r\n- Share them with others" })
    String ExportAction_ExportAction_tt();

    @Default(lngs = { "en" }, values = { "Import Rules from *.jdfilter (*.jdregexfilter) files" })
    String ImportAction_tt();

    @Default(lngs = { "en" }, values = { "Really delete all selected rules??" })
    String RemoveAction_actionPerformed_rly_msg();

    @Default(lngs = { "en" }, values = { "Offline Files" })
    String LinkFilterSettings_DefaultFilterList_getDefaultValue_();

    @Default(lngs = { "en" }, values = { "Offline Files" })
    String LinkCollector_addCrawledLink_offlinepackage();

    @Default(lngs = { "en" }, values = { "Various Files" })
    String LinkCollector_addCrawledLink_variouspackage();

    @Default(lngs = { "en" }, values = { "%s1" })
    String LinkCollector_archiv(String cleanFileName);

    @Default(lngs = { "en" }, values = { "Split Package %s1" })
    String SetDownloadFolderAction_actionPerformed_(String pkg);

    @Default(lngs = { "en" }, values = { "Change Download Folder for whole Package %s1, or only for %s2 selected link(s)" })
    String SetDownloadFolderAction_msg(String name, int num);

    @Default(lngs = { "en" }, values = { "Package" })
    String SetDownloadFolderAction_yes();

    @Default(lngs = { "en" }, values = { "Selection" })
    String SetDownloadFolderAction_no();

    @Default(lngs = { "en" }, values = { "Downloads are in progress!" })
    String DownloadWatchDog_onShutdownRequest_();

    @Default(lngs = { "en" }, values = { "Non resumable downloads are in progress!" })
    String DownloadWatchDog_onShutdownRequest_nonresumable();

    @Default(lngs = { "en" }, values = { "Do you want to stop running downloads to exit JDownloader?" })
    String DownloadWatchDog_onShutdownRequest_msg();

    @Default(lngs = { "en" }, values = { "LinkCollector is still in progress!" })
    String LinkCollector_onShutdownRequest_();

    @Default(lngs = { "en" }, values = { "Extraction is still in progress!" })
    String Extraction_onShutdownRequest_();

    @Default(lngs = { "en" }, values = { "Abort Extraction?" })
    String Extraction_onShutdownRequest_msg();

    @Default(lngs = { "en" }, values = { "Do you want to stop LinkCollector?" })
    String LinkCollector_onShutdownRequest_msg();

    @Default(lngs = { "en" }, values = { "Yes" })
    String literally_yes();

    @Default(lngs = { "en" }, values = { "Starting Downloads" })
    String Main_run_autostart_();

    @Default(lngs = { "en" }, values = { "Downloads will start a few seconds..." })
    String Main_run_autostart_msg();

    @Default(lngs = { "en" }, values = { "Start NOW!" })
    String Mainstart_now();

    @Default(lngs = { "en" }, values = { "Start Downloads" })
    String StartDownloadsAction_createTooltip_();

    @Default(lngs = { "en" }, values = { "Create Subfolder by Packagename" })
    String PackagizerSettings_folderbypackage_rule_name();

    @Default(lngs = { "en" }, values = { "Adopt folder structure" })
    String PackagizerSettings_folderbyplugin_rule_name2();

    @DescriptionForTranslationEntry("All words and all variants for the word 'password' should be placed here, seperated by a |. Example: passwort|pass|pw")
    @Default(lngs = { "en" }, values = { "пароль|пасс|pa?s?w|passwort|password|passw?|pw" })
    String pattern_password();

    @Default(lngs = { "en" }, values = { "Delete Container Files?" })
    String AddContainerAction_delete_container_title();

    @Default(lngs = { "en" }, values = { "Do you want to delete %s1 after adding the link(s) to JDownloader?" })
    String AddContainerAction_delete_container_msg(String list);

    @Default(lngs = { "en" }, values = { "Sort Linkgrabber?" })
    String getNextSortIdentifier_sort_warning_rly_title_();

    @Default(lngs = { "en" }, values = { "Do you really want to sort all Links and Packages on '%s1'?" })
    String getNextSortIdentifier_sort_warning_rly_msg(String string);

    @Default(lngs = { "en" }, values = { "Skipped" })
    String DownloadLink_setSkipped_statusmessage();

    @Default(lngs = { "en" }, values = { "More Actions..." })
    String OptionalContainer_OptionalContainer();

    @Default(lngs = { "en" }, values = { "Access to Captcha Settings" })
    String CaptchaQuickSettingsContainer_CaptchaQuickSettingsContainer();

    @Default(lngs = { "en" }, values = { "Skipped - Captcha is required" })
    String DownloadLink_setSkipped_statusmessage_captcha();

    @Default(lngs = { "en" }, values = { "Skipped - Disk is full" })
    String DownloadLink_setSkipped_statusmessage_disk_full();

    @Default(lngs = { "en" }, values = { "Invalid download directory" })
    String DownloadLink_setSkipped_statusmessage_invalid_path();

    @Default(lngs = { "en" }, values = { "Skipped - Account is missing" })
    String DownloadLink_setSkipped_statusmessage_account();

    @Default(lngs = { "en" }, values = { "Skipped - File already exists" })
    String DownloadLink_setSkipped_statusmessage_file_exists();

    @Default(lngs = { "en" }, values = { "Skipped - Too many retries" })
    String DownloadLink_setSkipped_statusmessage_toomanyretries();

    @Default(lngs = { "en" }, values = { "Skipped - No connection available" })
    String DownloadLink_setSkipped_statusmessage_noconnectionavailable();

    @Default(lngs = { "en" }, values = { "Proxy Authentication failed" })
    String plugins_errors_proxy_auth();

    @Default(lngs = { "en" }, values = { "Audio files: mp3, wav, ogg, mid,..." })
    String audiofilter_description();

    @Default(lngs = { "en" }, values = { "Hash files: md5, sha256, sfv,..." })
    String hashfilter_description();

    @Default(lngs = { "en" }, values = { "Archive Files: rar, zip,..." })
    String archive_description();

    @Default(lngs = { "en" }, values = { "Video Files: mp4, avi, mov,..." })
    String video_description();

    @Default(lngs = { "en" }, values = { "Image Files: jpg, png, gif,..." })
    String image_description();

    @Default(lngs = { "en" }, values = { "Doc Files: txt, rtf, doc,..." })
    String doc_description();

    @Default(lngs = { "en" }, values = { "All files except audio, archive, video & image files" })
    String other_files_description();

    @Default(lngs = { "en" }, values = { "Disable Archive *.rev Files" })
    String DisableRevFilesPackageRulee_rule_name();

    @Default(lngs = { "en" }, values = { "Disable Parity Archive *.par(2) Files" })
    String DisableParFilesPackageRulee_rule_name();

    @Default(lngs = { "en" }, values = { "Your 'My JDownloader' email has not been confirmed yet. \r\nPlease check your emails and click the confirmal Link" })
    String MyJDownloaderController_onError_account_unconfirmed();

    @Default(lngs = { "en" }, values = { "Your 'My JDownloader' logins are not correct. \r\nPlease check username/email and password!" })
    String MyJDownloaderController_onError_badlogins();

    @Default(lngs = { "en" }, values = { "JDownloader could not connect to the My JDownloader service: %s1" })
    String MyJDownloaderController_onError_unknown(String string);

    @Default(lngs = { "en" }, values = { "JDownloader could not connect to the My JDownloader service. Please update your JDownloader!" })
    String MyJDownloaderController_onError_outdated();

    @Default(lngs = { "en" }, values = { "Direct HTTP" })
    String LinkFilterSettings_DefaultFilterList_directhttp();

    @Default(lngs = { "en" }, values = { "Add Links Action" })
    String LinkSource_ADD_LINKS_DIALOG();

    @Default(lngs = { "en" }, values = { "Clipboard" })
    String LinkSource_CLIPBOARD();

    @Default(lngs = { "en" }, values = { "Add Container Action" })
    String LinkSource_ADD_CONTAINER_ACTION();

    @Default(lngs = { "en" }, values = { "Start Parameter" })
    String LinkSource_START_PARAMETER();

    @Default(lngs = { "en" }, values = { "Dock (Mac only)" })
    String LinkSource_MAC_DOCK();

    @Default(lngs = { "en" }, values = { "MyJDownloader" })
    String LinkSource_MYJD();

    @Default(lngs = { "en" }, values = { "Click'n'Load" })
    String LinkSource_CNL();

    @Default(lngs = { "en" }, values = { "Flashgot" })
    String LinkSource_FLASHGOT();

    @Default(lngs = { "en" }, values = { "Toolbar" })
    String LinkSource_TOOLBAR();

    @Default(lngs = { "en" }, values = { "Paste Links Action" })
    String LinkSource_PASTE_LINKS_ACTION();

    @Default(lngs = { "en" }, values = { "Drag&Drop Action" })
    String LinkSource_DRAG_DROP_ACTION();

    @Default(lngs = { "en" }, values = { "Downloaded Container" })
    String LinkSource_DOWNLOADED_CONTAINER();

    @Default(lngs = { "en" }, values = { "Plugin outdated" })
    String AccountController_updateAccountInfo_status_plugin_defect();

    @Default(lngs = { "en" }, values = { "Logins wrong" })
    String AccountController_updateAccountInfo_status_logins_wrong();

    @Default(lngs = { "en" }, values = { "Cannot check Account" })
    String AccountController_updateAccountInfo_status_uncheckable();

    @Default(lngs = { "en" }, values = { "No Traffic left" })
    String AccountController_updateAccountInfo_status_traffic_reached();

    @Default(lngs = { "en" }, values = { "Don't delete any files" })
    String DeleteOption_no_delete();

    @Default(lngs = { "en" }, values = { "Move files to Recycle if possible" })
    String DeleteOption_recycle();

    @Default(lngs = { "en" }, values = { "Delete files from Harddisk" })
    String DeleteOption_final_delete();

    @Default(lngs = { "en" }, values = { "Convert Cryptload *.clr Reconnect Script to JDownloader LiveHeader" })
    String convert_CLR_Reconnect_to_jdownloader();

    @Default(lngs = { "en" }, values = { "Ask me" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_ASK();

    @Default(lngs = { "en" }, values = { "Process Video only" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_VIDEO_ONLY();

    @Default(lngs = { "en" }, values = { "Process full Play-List" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_PLAYLIST_ONLY();

    @Default(lngs = { "en" }, values = { "Do nothing" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_NOTHING();

    @Default(lngs = { "en" }, values = { "Ask me" })
    String YoutubeDash_IfUrlisAPlaylistAction_ASK();

    @Default(lngs = { "en" }, values = { "Process full Play-List/Channel-List" })
    String YoutubeDash_IfUrlisAPlaylistAction_PROCESS();

    @Default(lngs = { "en" }, values = { "Do nothing" })
    String YoutubeDash_IfUrlisAPlaylistAction_NOTHING();

    @Default(lngs = { "en" }, values = { "Media Type (Video, Audio, Image, Subtitles, ..." })
    String YoutubeDash_GroupLogic_BY_MEDIA_TYPE();

    @Default(lngs = { "en" }, values = { "File Extension (MP4, AAC, MP3, ..." })
    String YoutubeDash_GroupLogic_BY_FILE_TYPE();

    @Default(lngs = { "en" }, values = { "Disabled -  Don't Group" })
    String YoutubeDash_GroupLogic_NO_GROUP();

    @Default(lngs = { "en" }, values = { "Skipped - Restart Required" })
    String DownloadLink_setSkipped_statusmessage_update_restart();

    @Default(lngs = { "en" }, values = { "Skipped - FFmpeg™ missing" })
    String DownloadLink_setSkipped_statusmessage_ffmpeg();

    @Default(lngs = { "en" }, values = { "Any Extension" })
    String LinkSource_EXTENSION();

    @Default(lngs = { "en" }, values = { "Proxy connection failed" })
    String plugins_errors_proxy_connection();

    @Default(lngs = { "en" }, values = { "Proxy authentication is required to connect to %s1" })
    String ProxyController_updateProxy_proxy_auth_required_msg(String url);

    @Default(lngs = { "en" }, values = { "Proxy authentication is required!" })
    String ProxyController_updateProxy_proxy_auth_required_title();

    // TODO Remove unused code found by UCDetector
    // @Default(lngs = { "en", "de" }, values = {
    // "JDownloader cannot connect to %s1! Your Proxy Server requires authentication. \r\nCheck your credentials...",
    // "JDownloader kann nicht nach %s1 verbinden! Dein Proxyserver benötigt Anmeldedaten.\r\nBitte überprüfe die Zugangsdaten..." })
    // String ProxyController_updateProxy_proxy_auth_required_msg_updater(String host);

    // TODO Remove unused code found by UCDetector
    // @Default(lngs = { "en" }, values = { "Authentication failed" })
    // String ProxyController_updateProxy_baned_auth();
    @Default(lngs = { "en" }, values = { "How to use the Connection Manager: \r\nYou can add several proxies or gateways to this list. Use the filter option to enable or disable a proxy for a certain domain only. For Free Downloads, JDownloader will try to use all available proxies simultanous. For Premium Downloads, the first available Proxy will be used." })
    String gui_settings_proxy_description_new();

    // TODO Remove unused code found by UCDetector
    // @Default(lngs = { "en" }, values = { "No Gateway or Proxy found." })
    // String plugins_errors_proxy_connection_nogateway();
    @Default(lngs = { "en" }, values = { "No Gateway or Proxy found." })
    String AccountController_updateAccountInfo_no_gateway();

    @Default(lngs = { "en" }, values = { "Authentication missing for %s1" })
    String AuthExceptionGenericBan_toString(String proxy);

    @Default(lngs = { "en" }, values = { "Connection to %s1 failed" })
    String ConnectExceptionInPluginBan(String proxy);

    @Default(lngs = { "en" }, values = { "%s2: Authentication missing for %s1" })
    String AuthExceptionGenericBan_toString_plugin(String proxy, String plugin);

    @Default(lngs = { "en" }, values = { "%s2: Connection to %s1 failed" })
    String ConnectExceptionInPluginBan_plugin(String proxy, String plugin);

    @Default(lngs = { "en" }, values = { "Authentication is required!" })
    String Plugin_requestLogins_message();

    @Default(lngs = { "en" }, values = { "Wrong username!" })
    String plugins_errors_wrongusername();

    @Default(lngs = { "en" }, values = { "A username and a password is required to download this file. Please enter the credentials below." })
    String DirectHTTP_getBasicAuth_message();

    @Default(lngs = { "en" }, values = { "Moving file to %s1" })
    String MovePluginProgress(String string);

    @Default(lngs = { "en" }, values = { "Hide" })
    String lit_hide();

    @Default(lngs = { "en" }, values = { "Please wait..." })
    String lit_please_wait();

    @Default(lngs = { "en" }, values = { "Skipped - FFprobe™ missing" })
    String DownloadLink_setSkipped_statusmessage_ffprobe();

    @Default(lngs = { "en" }, values = { "Original Video" })
    String GenericVariants_ORIGINAL();

    @Default(lngs = { "en" }, values = { "Extract Mp3 Audio" })
    String GenericVariants_FLV_TO_MP3_();

    @Default(lngs = { "en" }, values = { "to obtain video stream details, and to split audio from a video stream" })
    String plugin_for_host_reason_for_ffmpeg_demux();

    @Default(lngs = { "en" }, values = { "FFmpeg™ failed" })
    String PluginForHost_handle_ffmpeg_conversion_failed();

    @Default(lngs = { "en" }, values = { "Extract AAC Audio" })
    String GenericVariants_DEMUX_AAC();

    @Default(lngs = { "en" }, values = { "Extract M4A Audio" })
    String GenericVariants_DEMUX_M4A();

    @Default(lngs = { "en" }, values = { "Extract Audio Stream" })
    String GenericVariants_DEMUX_GENERIC_AUDIO();

    @Default(lngs = { "en" }, values = { "Moving file..." })
    String MovePluginProgress_nodest();

    @Default(lngs = { "en" }, values = { "Custom" })
    String UrlDisplayType_CUSTOM();

    @Default(lngs = { "en" }, values = { "Referrer" })
    String UrlDisplayType_REFERRER();

    @Default(lngs = { "en" }, values = { "Source" })
    String UrlDisplayType_ORIGIN();

    @Default(lngs = { "en" }, values = { "Container" })
    String UrlDisplayType_CONTAINER();

    @Default(lngs = { "en" }, values = { "Data" })
    String UrlDisplayType_CONTENT();

    @Default(lngs = { "en" }, values = { "Provides the ability to custom override. This is based on 'plugin source code' and 'users plugin preferences'. eg, Youtube: Set Custom Url" })
    String UrlDisplayType_CUSTOM_description();

    @Default(lngs = { "en" }, values = { "The website where you found the links. Only available if you added through Browser Extensions or Click'n'Load" })
    String UrlDisplayType_REFERRER_description();

    @Default(lngs = { "en" }, values = { "Your actual input -  what you pasted to JDownloader" })
    String UrlDisplayType_ORIGIN_description();

    @Default(lngs = { "en" }, values = { "The nearest Linkprotector, or redirection website" })
    String UrlDisplayType_CONTAINER_description();

    @Default(lngs = { "en" }, values = { "The address to the actual content." })
    String UrlDisplayType_CONTENT_description();

    @Default(lngs = { "en" }, values = { "add them, too" })
    String ConfirmLinksContextAction_HandleOfflineLinksOptions_INCLUDE_OFFLINE();

    @Default(lngs = { "en" }, values = { "do NOT add them" })
    String ConfirmLinksContextAction_HandleOfflineLinksOptions_EXCLUDE_OFFLINE();

    @Default(lngs = { "en" }, values = { "do NOT add, but remove them from linkgrabber" })
    String ConfirmLinksContextAction_HandleOfflineLinksOptions_EXCLUDE_OFFLINE_AND_REMOVE();

    @Default(lngs = { "en" }, values = { "ask me every time" })
    String ConfirmLinksContextAction_HandleOfflineLinksOptions_ASK();

    @Default(lngs = { "en" }, values = { "use global (adv. config) settings: %s1" })
    String ConfirmLinksContextAction_HandleOfflineLinksOptions_GLOBAL(String str);

    @Default(lngs = { "en" }, values = { "Hide if downloads are not running" })
    String PauseDownloadsAction_getHideIfDownloadsAreStoppedTranslation();

    @Default(lngs = { "en" }, values = { "Hide if downloads are running" })
    String StartDownloadsAction_getHideIfDownloadsAreRunningTranslation_();

    @Default(lngs = { "en" }, values = { "Hide Action on MAC OS" })
    String ExitAction_getHideOnMacTranslation();

    @Default(lngs = { "en" }, values = { "Item is visible for selected Links" })
    String TableContext_getTranslationItemVisibleForSelections_();

    @Default(lngs = { "en" }, values = { "Item is visible for empty selections" })
    String TableContext_getTranslationItemVisibleForEmptySelection();

    @Default(lngs = { "en" }, values = { "Max. autobackup/backup_*.jd2backup files" })
    String BackupRestoreAction_getTranslationForMaxAutoBackupFiles();

    @Default(lngs = { "en" }, values = { "Max. cfg_backup_* folders..." })
    String BackupRestoreAction_getTranslationForMaxCFGBackupFolders();

    @Default(lngs = { "en" }, values = { "Key Modifier to toggle 'Delete Files'" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteFilesToggleModifier();

    @Default(lngs = { "en" }, values = { "Only Selected Links" })
    String GenericDeleteFromTableToolbarAction_getTranslationForOnlySelectedItems();

    @Default(lngs = { "en" }, values = { "Exclude filtered Links" })
    String GenericDeleteFromTableToolbarAction_getTranslationForIgnoreFiltered();

    @Default(lngs = { "en" }, values = { "Include Offline Links" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteOffline();

    @Default(lngs = { "en" }, values = { "Include finished Links" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteFinished();

    @Default(lngs = { "en" }, values = { "Include failed" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteFailed();

    @Default(lngs = { "en" }, values = { "Include disabled Links" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteDisabled();

    @Default(lngs = { "en" }, values = { "Include All Links" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteAll();

    @Default(lngs = { "en" }, values = { "Delete Mode" })
    String GenericDeleteFromTableToolbarAction_getTranslationForDeleteMode();

    @Default(lngs = { "en" }, values = { "Visible in Download Tab" })
    String ToolbarContext_getTranslationForVisibleInDownloadTab();

    @Default(lngs = { "en" }, values = { "Visible in Linkgrabber Tab" })
    String ToolbarContext_getTranslationForVisibleInLinkgrabberTab();

    @Default(lngs = { "en" }, values = { "Visible in All Tab" })
    String ToolbarContext_getTranslationForVisibleInAllTabs();

    @Default(lngs = { "en" }, values = { "Path A to *.jdproxies File" })
    String SetProxySetupAction_getTranslationForPath();

    @Default(lngs = { "en" }, values = { "Simple single rename (table)" })
    String RenameAction_getTranslationForSimpleMode();

    @Default(lngs = { "en" }, values = { "Simple input dialog" })
    String SetDownloadFolder_getTranslationForSimpleMode();

    @Default(lngs = { "en" }, values = { "Force change of priority" })
    String PriorityAction_getTranslationForForceMode();

    @Default(lngs = { "en" }, values = { "Key Modifier to toggle 'Bypass Rly? Dialog'" })
    String ByPassDialogSetup_getTranslationForByPassDialogToggleModifier();

    @Default(lngs = { "en" }, values = { "Bypass the 'Really?' Dialog" })
    String ByPassDialogSetup_getTranslationForBypassDialog();

    @Default(lngs = { "en" }, values = { "Add only selected Links" })
    String CollapseExpandContextAction_getTranslationForSelectionOnly();

    @Default(lngs = { "en" }, values = { "<html>Pattern for the Packages<br><ul><li>{name}</li><li>{newline}</li><li>{comment}</li><li>{filesize}</li><li>{type}</li><li>{path}</li></ul></html>" })
    String CopyGenericContextAction_getTranslationForPatternPackages_v2();

    @Default(lngs = { "en" }, values = { "<html>Pattern for the Links<br><ul><li>{name}</li><li>{newline}</li><li>{comment}</li><li>{sha256}</li><li>{md5}</li><li>{filesize}</li><li>{url}</li><li>{type}</li><li>{path}</li></ul></html>" })
    String CopyGenericContextAction_getTranslationForPatternLinks_v2();

    @Default(lngs = { "en" }, values = { "Smart Selection" })
    String CopyGenericContextAction_getTranslationForSmartSelection();

    @Default(lngs = { "en" }, values = { "Max Chunks" })
    String GenericChunksAction_getTranslationChunks();

    @Default(lngs = { "en" }, values = { "Key Modifier to toggle 'Delete Files'" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteFilesToggleModifier();

    @Default(lngs = { "en" }, values = { "Delete Mode" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteMode();

    @Default(lngs = { "en" }, values = { "Affected Links: All" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteAll();

    @Default(lngs = { "en" }, values = { "Affected Links: Disabled" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteDisabled();

    @Default(lngs = { "en" }, values = { "Affected Links: Failed" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteFailed();

    @Default(lngs = { "en" }, values = { "Affected Links: Finished" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteFinished();

    @Default(lngs = { "en" }, values = { "Affected Links: Offline" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteOffline();

    @Default(lngs = { "en" }, values = { "Exclude filtered Links" })
    String GenericDeleteFromDownloadlistAction_getTranslationForIgnoreFiltered();

    @Default(lngs = { "en" }, values = { "Add package at" })
    String MergeToPackageAction_getTranslationForLocation();

    @Default(lngs = { "en" }, values = { "Use latest selected path as default one" })
    String MergeToPackageAction_getTranslationForLastPathDefault();

    @Default(lngs = { "en" }, values = { "Expand the new package after creation" })
    String MergeToPackageAction_getTranslationForExpandNewPackage();

    @Default(lngs = { "en" }, values = { "Merge Packages before splitting?" })
    String SplitPackagesByHost_getTranslationForMergePackages();

    @Default(lngs = { "en" }, values = { "If Merging, ask for new Downloadfolder and package name?" })
    String SplitPackagesByHost_getTranslationForAskForNewDownloadFolderAndPackageName();

    @Default(lngs = { "en" }, values = { "Add package at" })
    String SplitPackagesByHost_getTranslationForLocation();

    @Default(lngs = { "en" }, values = { "The end of the list" })
    String LocationInList_getLabel_END_OF_LIST();

    @Default(lngs = { "en" }, values = { "The top of the list" })
    String LocationInList_getLabel_TOP_OF_LIST();

    @Default(lngs = { "en" }, values = { "After selection" })
    String LocationInList_getLabel_AFTER_SELECTION();

    @Default(lngs = { "en" }, values = { "Before selection" })
    String LocationInList_getLabel_BEFORE_SELECTION();

    @Default(lngs = { "en" }, values = { "Only Visible if there is filtered stuff" })
    String AddFilteredStuffAction_getTranslationForOnlyVisibleIfThereIsFilteredStuff();

    @Default(lngs = { "en" }, values = { "Cancel all running Crawler Jobs" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForCancelLinkcrawlerJobs();

    @Default(lngs = { "en" }, values = { "Reset Table Sorting" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForResetTableSorter();

    @Default(lngs = { "en" }, values = { "Clear Searchfield" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForClearSearchFilter();

    @Default(lngs = { "en" }, values = { "Clear Filtered Links" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForClearFilteredLinks();

    @Default(lngs = { "en" }, values = { "Affected Links: All" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForDeleteAll();

    @Default(lngs = { "en" }, values = { "Affected Links: Disabled" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForDeleteDisabled();

    @Default(lngs = { "en" }, values = { "Affected Links: Offline" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForDeleteOffline();

    @Default(lngs = { "en" }, values = { "Exclude filtered Links" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForIgnoreFiltered();

    @Default(lngs = { "en" }, values = { "Include Selected Links" })
    String IncludedSelectionSetup_getTranslationForIncludeSelectedLinks();

    @Default(lngs = { "en" }, values = { "Include Unselected Links" })
    String IncludedSelectionSetup_getTranslationForIncludeUnselectedLinks();

    @Default(lngs = { "en" }, values = { "Deep Decrypt" })
    String PasteLinksAction_getTranslationForDeepDecryptEnabled();

    @Default(lngs = { "en" }, values = { "Hide the Popupmenu after clicking" })
    String ToggleAppAction_getTranslationForHidePopupOnClick();

    @Default(lngs = { "en" }, values = { "Autostart: Automode (Quicksettings)" })
    String AutoStartOptions_AUTO();

    @Default(lngs = { "en" }, values = { "Autostart: Never start Downloads" })
    String AutoStartOptions_DISABLED();

    @Default(lngs = { "en" }, values = { "Autostart: Always start Downloads" })
    String AutoStartOptions_ENABLED();

    @Default(lngs = { "en" }, values = { "CTRL Toggle Enabled" })
    String ConfirmLinksContextAction_getTranslationForCtrlToggle();

    @Default(lngs = { "en" }, values = { "Force Downloads" })
    String ConfirmLinksContextAction_getTranslationForForceDownloads();

    @Default(lngs = { "en" }, values = { "Enabled Prioritychange" })
    String ConfirmLinksContextAction_getTranslationForAssignPriorityEnabled();

    @Default(lngs = { "en" }, values = { "Download Priority:" })
    String ConfirmLinksContextAction_getTranslationForPiority();

    @Default(lngs = { "en" }, values = { "If the selection contains offline links..." })
    String ConfirmLinksContextAction_getTranslationForHandleOffline();

    @Default(lngs = { "en" }, values = { "Clear Linkgrabber after adding links" })
    String ConfirmLinksContextAction_getTranslationForClearListAfterConfirm();

    @Default(lngs = { "en" }, values = { "Autostart Downloads afterwards" })
    String ConfirmLinksContextAction_getTranslationForAutoStart();

    @Default(lngs = { "en" }, values = { "Add only selected Links" })
    String ConfirmLinksContextAction_getTranslationForSelectionOnly();

    @Default(lngs = { "en" }, values = { "Deep Decrypt" })
    String PasteContextLinksAction_getTranslationForDeepDecryptEnabled();

    @Default(lngs = { "en" }, values = { "Link is already in Downloadlist" })
    String DOWNLOAD_LIST_DUPE();

    @Default(lngs = { "en" }, values = { "Already in Downloadlist" })
    String LinkFilterSettings_DefaultFilterList_dupes();

    @Default(lngs = { "en" }, values = { "Delete Duplicates" })
    String GenericDeleteFromLinkgrabberAction_getTranslationForDeleteDupesEnabled();

    @Default(lngs = { "en" }, values = { "add them, too" })
    String ConfirmLinksContextAction_HandleDupesLinksOptions_INCLUDE();

    @Default(lngs = { "en" }, values = { "do NOT add them" })
    String ConfirmLinksContextAction_HandleDupesLinksOptions_EXCLUDE();

    @Default(lngs = { "en" }, values = { "do NOT add, but remove them from linkgrabber" })
    String ConfirmLinksContextAction_HandleDupesLinksOptions_EXCLUDE_AND_REMOVE();

    @Default(lngs = { "en" }, values = { "ask me every time" })
    String ConfirmLinksContextAction_HandleDupesLinksOptions_ASK();

    @Default(lngs = { "en" }, values = { "use global (adv. config) settings: %s1" })
    String ConfirmLinksContextAction_HandleDupesLinksOptions_GLOBAL(String str);

    @Default(lngs = { "en" }, values = { "if the selection contains duplicate links..." })
    String ConfirmLinksContextAction_getTranslationForHandleDupes();

    @Default(lngs = { "en" }, values = { "Affected Links: Finished Package" })
    String GenericDeleteFromDownloadlistAction_getTranslationForDeleteFinishedPackage();

    @Default(lngs = { "en" }, values = { "This Download is not available in your country" })
    String CountryIPBlockException_createCandidateResult();

    @Default(lngs = { "en" }, values = { "Free Account" })
    String AccountType_free();

    @Default(lngs = { "en" }, values = { "Premium Account" })
    String AccountType_premium();

    @Default(lngs = { "en" }, values = { "Lifetime Account" })
    String AccountType_lifetime();

    @Default(lngs = { "en" }, values = { "Unknown Account" })
    String AccountType_unknown();

    @Default(lngs = { "en" }, values = { "Current Path" })
    String DownloadFolderChooserDialogDefaultPath_CURRENT_PATH();

    @Default(lngs = { "en" }, values = { "The global Download Directory: %s1" })
    String DownloadFolderChooserDialogDefaultPath_GLOBAL_DOWNLOAD_DIRECTORY(String dir);

    @Default(lngs = { "en" }, values = { "Latest used path" })
    String DownloadFolderChooserDialogDefaultPath_LAST_USED_PATH();

    @Default(lngs = { "en" }, values = { "Hidden (Automode)" })
    String DonateButtonState_AUTO_HIDDEN();

    @Default(lngs = { "en" }, values = { "Visible (Automode)" })
    String DonateButtonState_AUTO_VISIBLE();

    @Default(lngs = { "en" }, values = { "Visible (Usermode)" })
    String DonateButtonState_CUSTOM_VISIBLE();

    @Default(lngs = { "en" }, values = { "Hidden (Usermode)" })
    String DonateButtonState_CUSTOM_HIDDEN();

    @Default(lngs = { "en" }, values = { "Google 2-Step Verification" })
    String Google_helper_2factor_sms_dialog_title();

    @Default(lngs = { "en" }, values = { "Google sent a verification code to you mobile phone.\r\nNumber: %s1\r\nPlease enter this code here..." })
    String Google_helper_2factor_sms_dialog_msg(String number);

    @Default(lngs = { "en" }, values = { "Google wants you to accept new Privacy Policy." })
    String google_helper_privacy_update_title();

    @Default(lngs = { "en" }, values = { "Please login to your google account '%s1'. After the login, Google will ask you to accept the new privacy policy. Continue after you did this." })
    String google_helper_privacy_update_message(String account);

    @Default(lngs = { "en" }, values = { "Privacy reminder check failed.\r\nPlease login to your google account '%s1'. After the login, Google will ask you to accept the new privacy policy. Continue after you did this." })
    String google_helper_privacy_update_message_retry(String user);

    @Default(lngs = { "en" }, values = { "Google recovery Email verification" })
    String google_email_verification_title();

    @Default(lngs = { "en" }, values = { "Something seems a bit different about the way you're trying to sign in. To verify that it's you, please enter your recovery email address\r\n%s1" })
    String google_email_verification_message(String example);

    @Default(lngs = { "en" }, values = { "Google password change required" })
    String google_password_change_title();

    @Default(lngs = { "en" }, values = { "Google detected suspicious activity on your '%s1' Google Account.\r\nPlease choose a new password to finish signing in." })
    String google_password_change_message(String acc);

    @Default(lngs = { "en" }, values = { "New Password" })
    String newpassworddialog_password();

    @Default(lngs = { "en" }, values = { "Re-enter new Password" })
    String newpassworddialog_password_verify();

    @Default(lngs = { "en" }, values = { "A problem occured during the Google signup:\r\n%s1" })
    String google_error(String error);

    @Default(lngs = { "en" }, values = { "Start ALL Downloads (clear forced flag)" })
    String StartDownloadsAction_forced_createTooltip_();

    @Default(lngs = { "en" }, values = { "Wait for %s1: %s2" })
    String WaitForTrackerSlotPluginProcess(String message, String formatSeconds);

    @Default(lngs = { "en" }, values = { "Usenet" })
    String LazyHostPlugin_FEATURE_USENET();

    @Default(lngs = { "en" }, values = { "Multihoster" })
    String LazyHostPlugin_FEATURE_MULTIHOST();

    @Default(lngs = { "en" }, values = { "Generic" })
    String LazyHostPlugin_FEATURE_GENERIC();

    @Default(lngs = { "en" }, values = { "This service supports UseNet downloads (*.nzb files)" })
    String LazyHostPlugin_FEATURE_USENET_TOOLTIP();

    @Default(lngs = { "en" }, values = { "This service supports downloads form multiple different hosters" })
    String LazyHostPlugin_FEATURE_MULTIHOST_TOOLTIP();

    @Default(lngs = { "en" }, values = { "This plugin implements generic support. Please use the dedicated plugin instead. " })
    String LazyHostPlugin_FEATURE_GENERIC_TOOLTIP();

    @Default(lngs = { "en" }, values = { "Vorbis Spatial 4 Channel" })
    String AudioCodec_vorbis_spatial();

    @Default(lngs = { "en" }, values = { "AAC Spatial 6 Channel" })
    String AudioCodec_aac_spatial();

    @Default(lngs = { "en" }, values = { "Spatial" })
    String YOUTUBE_surround();

    @Default(lngs = { "en" }, values = { "3D" })
    String Projection_3D();

    @Default(lngs = { "en" }, values = { "2D" })
    String Projection_Normal();

    @Default(lngs = { "en" }, values = { "360°" })
    String Projection_Spherical();

    @Default(lngs = { "en" }, values = { "3D 360°" })
    String Projection_Spherical3D();

    @Default(lngs = { "en" }, values = { "3D HOU 360° Spherical" })
    String Projection_Spherical3D_tt();

    @Default(lngs = { "en" }, values = { "360° Spherical" })
    String Projection_Spherical_tt();

    @Default(lngs = { "en" }, values = { "2D - Normal" })
    String Projection_Normal_tt();

    @Default(lngs = { "en" }, values = { "Stereoscopic 3D SBS" })
    String Projection_3D_tt();

    @Default(lngs = { "en" }, values = { "Spatial Multichannel Audio" })
    String youtube_spatial();

    @Default(lngs = { "en" }, values = { "Video Codec" })
    String lit_video_codec();

    @Default(lngs = { "en" }, values = { "Video Framerate" })
    String lit_video_framerate();

    @Default(lngs = { "en" }, values = { "Resolution" })
    String lit_resolution();

    @Default(lngs = { "en" }, values = { "Audio Bitrate" })
    String lit_audio_bitrate();

    @Default(lngs = { "en" }, values = { "Audio Codec" })
    String lit_audio_codec();

    @Default(lngs = { "en" }, values = { "Filetype" })
    String lit_file_container();

    @Default(lngs = { "en" }, values = { "Demux Audio" })
    String youtube_demux();

    @Default(lngs = { "en" }, values = { "Segment Stream" })
    String youtube_segment();

    @Default(lngs = { "en" }, values = { "PhantomJS is missing!" })
    String DownloadLink_setSkipped_statusmessage_phantom();

    @Default(lngs = { "en" }, values = { "Please open the login page in your browser and follow the login steps.\r\nJDownloader will continue as soon as you finished the login." })
    String OAUTH_CHALLENGE_EXPLAIN();

    @Default(lngs = { "en" }, values = { "Prefer reconnect" })
    String lit_prefer_reconnect();

    @Default(lngs = { "en" }, values = { "Prefer SSL/HTTPS" })
    String lit_prefer_ssl();

    @Default(lngs = { "en" }, values = { "Add Subtitles" })
    String lit_add_subtitles();

    @Default(lngs = { "en" }, values = { "Add only the best video quality" })
    String lit_add_only_the_best_video_quality();

    @Default(lngs = { "en" }, values = { "Add only the best video quality within user selected formats" })
    String lit_add_only_the_best_video_quality_within_user_selected_formats();

    @Default(lngs = { "en" }, values = { "Add unknown formats (recommended)" })
    String lit_add_unknown_formats();

    @Default(lngs = { "en" }, values = { "Port" })
    String lit_port();

    @Default(lngs = { "en" }, values = { "Host" })
    String lit_host();

    @Default(lngs = { "en" }, values = { "SSL Enabled" })
    String lit_ssl_enabled();

    @Default(lngs = { "en" }, values = { "Connections" })
    String lit_connections();

    @Default(lngs = { "en" }, values = { "Username" })
    String lit_username();

    @Default(lngs = { "en" }, values = { "Time account" })
    String plugins_simplypremiumcom_ACCOUNT_TYPE_TIME();

    @Default(lngs = { "en" }, values = { "No" })
    String lit_no();

    @Default(lngs = { "en" }, values = { "Volume account" })
    String plugins_simplypremiumcom_ACCOUNT_TYPE_VOLUME();

    @Default(lngs = { "en" }, values = { "Invalid username/password!\r\nYou're sure that the username and password you entered are correct? Some hints:\r\n1. If your password contains special characters, change it (remove them) and try again!\r\n2. Type in your username/password by hand without copy & paste." })
    String plugins_tb7pl_PREMIUM_ERROR();

    @Default(lngs = { "en" }, values = { "Expired" })
    String lit_expired();

    @Default(lngs = { "en" }, values = { "Unsupported account type!\r\nIf you think this message is incorrect or it makes sense to add support for this account type\r\ncontact us via our support forum." })
    String plugins_tb7pl_UNSUPPORTED_PREMIUM();

    @Default(lngs = { "en" }, values = { "Plugin Defect. Please contact support@jdownloader.org" })
    String lit_plugin_defect_pls_contact_support();

    @Default(lngs = { "en" }, values = { "Unknown" })
    String lit_unknown();

    @Default(lngs = { "en" }, values = { "Traffic Left" })
    String lit_traffic_left();

    @Default(lngs = { "en" }, values = { "Unlimited" })
    String lit_unlimited();

    @Default(lngs = { "en" }, values = { "Host is temporarily unavailable via %s1" })
    String plugins_tb7pl_HOSTER_UNAVAILABLE(String string);

    @Default(lngs = { "en" }, values = { "Download limit exceeded!" })
    String lit_download_limit_exceeded();

    @Default(lngs = { "en" }, values = { "Tb7 reports the link is as inactive!" })
    String plugins_tb7pl_LINK_INACTIVE();

    @Default(lngs = { "en" }, values = { "Retry in few seconds" })
    String lit_retry_in_a_few_seconds();

    @Default(lngs = { "en" }, values = { "Previously generated Link expired!" })
    String plugins_tb7pl_LINK_EXPIRED();

    @Default(lngs = { "en" }, values = { "Traffic limit reached" })
    String lit_traffic_limit_reached();

    @Default(lngs = { "en" }, values = { "Unable to handle this errorcode!" })
    String plugins_tb7pl_UNKNOWN_ERROR();

    @Default(lngs = { "en" }, values = { "Use for Recaptcha 1 (write off images)" })
    String plugins_google_use_for_recaptcha1();

    @Default(lngs = { "en" }, values = { "JDownloader can use this google account to improve Recaptcha Image Challenges. If you use this account for Recaptcha, you probably will get easier to solve, and less captcha challenges." })
    String plugins_google_recaptcha2_description();

    @Default(lngs = { "en" }, values = { "Use for Recaptcha 2 (click on image category)" })
    String plugins_google_use_for_recaptcha2();
}