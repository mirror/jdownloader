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

    @Default(lngs = { "en", "de" }, values = { "If a Proxy Server is required to access internet, please enter proxy data here. JDownloader is able to rotate several Proxies to avoid IP waittimes. Default Proxy is used for all connections that are not IP restricted.", "Falls ein Proxy benötigt wird um ins Internet zu verbinden, kann dieser hier eingetragen werden. Um IP Wartezeit zu vermeiden, können mehrere Proxy Server eingetragen werden. Der Defaultproxy wird für alle Verbindungen ohne IP Beschränkungen verwendet." })
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

    @Default(lngs = { "en" }, values = { "File exists" })
    String jd_controlling_SingleDownloadController_askexists_title();

    @Default(lngs = { "en" }, values = { "Could not clone the connection" })
    String download_error_message_connectioncopyerror();

    @Default(lngs = { "en" }, values = { "Wait %s1" })
    String gui_download_waittime_status2(Object s1);

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

    @Default(lngs = { "en" }, values = { "Proxy Authentication failed" })
    String plugins_errors_proxy_auth();

    @Default(lngs = { "en" }, values = { "Audio files: mp3, wav, ogg, mid,..." })
    String audiofilter_description();

    @Default(lngs = { "en" }, values = { "Archive Files: rar, zip,..." })
    String archive_description();

    @Default(lngs = { "en" }, values = { "Video Files: mp4, avi, mov,..." })
    String video_description();

    @Default(lngs = { "en" }, values = { "Image Files: jpg, png, gif,..." })
    String image_description();

    @Default(lngs = { "en" }, values = { "All files except audio, archive, video & image files" })
    String other_files_description();

    @Default(lngs = { "en" }, values = { "Disable Archive *.rev Files" })
    String DisableRevFilesPackageRulee_rule_name();

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

    @Default(lngs = { "en" }, values = { "Load Video only" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_VIDEO_ONLY();

    @Default(lngs = { "en" }, values = { "Load full playlist" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_PLAYLIST_ONLY();

    @Default(lngs = { "en" }, values = { "Load nothing" })
    String YoutubeDash_IfUrlisAVideoAndPlaylistAction_NOTHING();

    @Default(lngs = { "en" }, values = { "Media Type (Video, Audio, Image, Subtitles, ..." })
    String YoutubeDash_GroupLogic_BY_MEDIA_TYPE();

    @Default(lngs = { "en" }, values = { "File Extension (MP4, AAC, MP3, ..." })
    String YoutubeDash_GroupLogic_BY_FILE_TYPE();

    @Default(lngs = { "en" }, values = { "Disabled -  Don't Group" })
    String YoutubeDash_GroupLogic_NO_GROUP();

}