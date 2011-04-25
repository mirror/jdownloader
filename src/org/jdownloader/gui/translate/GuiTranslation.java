package org.jdownloader.gui.translate;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.TranslateInterface;

@Defaults(lngs = { "en" })
public interface GuiTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "Grabbed %s1 link(s) in %s2 Package(s)" })
    String gui_linkgrabber_finished(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "Loading with Premium" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_premium();

    @Default(lngs = { "en" }, values = { "alt" })
    String jd_gui_swing_ShortCuts_key_alt();

    @Default(lngs = { "en" }, values = { "Please enter Regex for IPCheck here" })
    String gui_config_download_ipcheck_regex_default();

    @Default(lngs = { "en" }, values = { "Version" })
    String gui_column_version();

    @Default(lngs = { "en" }, values = { "LinkGrabber" })
    String gui_config_gui_linkgrabber();

    @Default(lngs = { "en" }, values = { "Barrier-Free" })
    String gui_config_gui_barrierfree();

    @Default(lngs = { "en" }, values = { "%s1 links" })
    String gui_downloadlist_delete_links(Object s1);

    @Default(lngs = { "en" }, values = { "Put Linkgrabberbuttons above table" })
    String gui_config_linkgrabber_controlposition();

    @Default(lngs = { "en" }, values = { "Archive passwords" })
    String jd_gui_swing_jdgui_settings_panels_PasswordList_general_title();

    @Default(lngs = { "en" }, values = { "All options and settings for JDownloader" })
    String jd_gui_swing_jdgui_views_configurationview_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Font Size [%]" })
    String gui_config_gui_font_size();

    @Default(lngs = { "en" }, values = { "%s1 files" })
    String gui_downloadlist_delete_files(Object s1);

    @Default(lngs = { "en" }, values = { "Open new packages by default" })
    String jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages();

    @Default(lngs = { "en" }, values = { "Edit Directory" })
    String gui_table_contextmenu_editdownloaddir();

    @Default(lngs = { "en" }, values = { "Downloadlink" })
    String gui_fileinfopanel_link();

    @Default(lngs = { "en" }, values = { "Use balanced IP-Check" })
    String gui_config_download_ipcheck_balance();

    @Default(lngs = { "en" }, values = { "%s1 -- Unlimited traffic! You can download as much as you want to." })
    String gui_premiumstatus_unlimited_traffic_tooltip(Object s1);

    @Default(lngs = { "en" }, values = { "FileSize" })
    String gui_treetable_size();

    @Default(lngs = { "en" }, values = { "Added date" })
    String gui_treetable_added();

    @Default(lngs = { "en" }, values = { "Copy URL" })
    String gui_table_contextmenu_copyLink();

    @Default(lngs = { "en" }, values = { "Remove finished downloads ..." })
    String gui_config_general_todowithdownloads();

    @Default(lngs = { "en" }, values = { "LinkGrabber operations pending..." })
    String gui_linkgrabber_pc_linkgrabber();

    @Default(lngs = { "en" }, values = { "You disabled the IP-Check. This will increase the reconnection times dramatically!\r\n\r\nSeveral further modules like Reconnect Recorder are disabled." })
    String jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_message();

    @Default(lngs = { "en" }, values = { "Load DLC file" })
    String gui_filechooser_loaddlc();

    @Default(lngs = { "en" }, values = { "Total size" })
    String jd_gui_swing_jdgui_views_info_DownloadInfoPanel_size();

    @Default(lngs = { "en" }, values = { "Local IP" })
    String jd_gui_swing_dialog_ProxyDialog_localip();

    @Default(lngs = { "en" }, values = { "Resumable download" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_resume();

    @Default(lngs = { "en" }, values = { "Use" })
    String jd_gui_swing_jdgui_settings_panels_gui_ToolbarController_column_use();

    @Default(lngs = { "en" }, values = { "dd.MM.yy HH:mm" })
    String org_jdownloader_gui_views_downloads_columns_DateColumn_dateFormat();

    @Default(lngs = { "en" }, values = { "Max. Speed" })
    String gui_statusbar_speed();

    @Default(lngs = { "en" }, values = { "Change Package Name" })
    String gui_table_contextmenu_editpackagename();

    @Default(lngs = { "en" }, values = { "New Package Name" })
    String gui_linklist_editpackagename_message();

    @Default(lngs = { "en" }, values = { "%s1 online" })
    String gui_linkgrabber_packageonlinepercent(Object s1);

    @Default(lngs = { "en" }, values = { "PremiumPoints" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_premiumpoints();

    @Default(lngs = { "en" }, values = { "IP Filter RegEx" })
    String gui_config_download_ipcheck_regex();

    @Default(lngs = { "en" }, values = { "Style (Restart required)" })
    String gui_config_gui_plaf();

    @Default(lngs = { "en" }, values = { "Please send this loglink to your supporter" })
    String gui_logupload_message();

    @Default(lngs = { "en" }, values = { "JDownloader Installation" })
    String installer_gui_title();

    @Default(lngs = { "en" }, values = { "Create DLC" })
    String gui_table_contextmenu_dlc();

    @Default(lngs = { "en" }, values = { "Port" })
    String gui_column_port();

    @Default(lngs = { "en" }, values = { "alt Gr" })
    String jd_gui_swing_ShortCuts_key_altGr();

    @Default(lngs = { "en" }, values = { "Reset Dialog Information" })
    String gui_config_gui_resetdialogs2();

    @Default(lngs = { "en" }, values = { "Select all" })
    String gui_textcomponent_context_selectall();

    @Default(lngs = { "en" }, values = { "Chart is loading or not available" })
    String plugins_config_premium_chartapi_caption_error2();

    @Default(lngs = { "en" }, values = { "Enter Encryption Password" })
    String jd_gui_swing_jdgui_menu_actions_BackupLinkListAction_password();

    @Default(lngs = { "en" }, values = { "Drop before '%s1'" })
    String gui_table_draganddrop_before(Object s1);

    @Default(lngs = { "en" }, values = { "Restart required!" })
    String jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_title();

    @Default(lngs = { "en" }, values = { "Please confirm!" })
    String jd_gui_userio_defaulttitle_confirm();

    @Default(lngs = { "en" }, values = { "Delete from list" })
    String gui_table_contextmenu_deletelist2();

    @Default(lngs = { "en" }, values = { "Speed: %s1/s" })
    String gui_fileinfopanel_linktab_speed(Object s1);

    @Default(lngs = { "en" }, values = { "Use Subdirectory" })
    String gui_linkgrabber_packagetab_chb_useSubdirectory();

    @Default(lngs = { "en" }, values = { "Show detailed container information on load" })
    String gui_config_showContainerOnLoadInfo();

    @Default(lngs = { "en" }, values = { "Add at top" })
    String gui_taskpanes_download_linkgrabber_config_addattop();

    @Default(lngs = { "en" }, values = { "Show license" })
    String jd_gui_swing_components_AboutDialog_license();

    @Default(lngs = { "en" }, values = { "Downloadspeed" })
    String jd_gui_swing_jdgui_views_info_DownloadInfoPanel_speed();

    @Default(lngs = { "en" }, values = { "Restart NOW!" })
    String jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_ok();

    @Default(lngs = { "en" }, values = { "All online" })
    String gui_linkgrabber_packageonlineall();

    @Default(lngs = { "en" }, values = { "Sort" })
    String gui_table_contextmenu_sort();

    @Default(lngs = { "en" }, values = { "offline" })
    String linkgrabber_onlinestatus_offline();

    @Default(lngs = { "en" }, values = { "Skip Link" })
    String system_download_triggerfileexists_skip();

    @Default(lngs = { "en" }, values = { "Remove" })
    String gui_table_contextmenu_remove();

    @Default(lngs = { "en" }, values = { "See or Upload the Log" })
    String jd_gui_swing_jdgui_views_log_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Comment" })
    String gui_fileinfopanel_packagetab_lbl_comment();

    @Default(lngs = { "en" }, values = { "Paste" })
    String gui_textcomponent_context_paste();

    @Default(lngs = { "en" }, values = { "Insert after '%s1'" })
    String gui_table_draganddrop_movepackageend(Object s1);

    @Default(lngs = { "en" }, values = { "Comment" })
    String gui_fileinfopanel_linktab_comment();

    @Default(lngs = { "en" }, values = { "File" })
    String jd_gui_skins_simple_simplegui_menubar_filemenu();

    @Default(lngs = { "en" }, values = { "Premium" })
    String gui_menu_premium();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_hoster();

    @Default(lngs = { "en" }, values = { "Automatic" })
    String jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages_automatic();

    @Default(lngs = { "en" }, values = { "(Autopackager)Replace dots and _ with spaces?" })
    String gui_config_linkgrabber_replacechars();

    @Default(lngs = { "en" }, values = { "Proxystatus" })
    String gui_column_proxystatus();

    @Default(lngs = { "en" }, values = { "Save to" })
    String gui_fileinfopanel_packagetab_lbl_saveto();

    @Default(lngs = { "en" }, values = { "Package(s)" })
    String jd_gui_swing_jdgui_views_info_DownloadInfoPanel_packages();

    @Default(lngs = { "en" }, values = { "Use all Hosts" })
    String jd_gui_swing_jdgui_settings_panels_ConfigPanelPlugin_useAll();

    @Default(lngs = { "en" }, values = { "Proxytype" })
    String gui_column_proxytype();

    @Default(lngs = { "en" }, values = { "Plugin" })
    String gui_column_plugin();

    @Default(lngs = { "en" }, values = { "Insert at the end of Package '%s1'" })
    String gui_table_draganddrop_insertinpackageend(Object s1);

    @Default(lngs = { "en" }, values = { "Max. Con." })
    String gui_statusbar_maxChunks();

    @Default(lngs = { "en" }, values = { "Create Subfolder with packagename if possible" })
    String gui_config_general_createsubfolders();

    @Default(lngs = { "en" }, values = { "Disable Premium?" })
    String dialogs_premiumstatus_global_title();

    @Default(lngs = { "en" }, values = { "Status" })
    String gui_treetable_status();

    @Default(lngs = { "en" }, values = { "This option needs a JDownloader restart." })
    String jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion();

    @Default(lngs = { "en" }, values = { "Stop after current Downloads" })
    String jd_gui_swing_jdgui_actions_ActionController_toolbar_control_stopmark_tooltip();

    @Default(lngs = { "en" }, values = { "ExpireDate" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_expiredate();

    @Default(lngs = { "en" }, values = { "Adding %s1 link(s) to LinkGrabber" })
    String gui_linkgrabber_adding(Object s1);

    @Default(lngs = { "en" }, values = { "Copy" })
    String jd_gui_swing_components_AboutDialog_copy();

    @Default(lngs = { "en" }, values = { "HTTP" })
    String jd_gui_swing_dialog_ProxyDialog_http();

    @Default(lngs = { "en" }, values = { "Split by hoster" })
    String gui_linkgrabberv2_splithoster();

    @Default(lngs = { "en" }, values = { "Open File" })
    String gui_table_contextmenu_openfile();

    @Default(lngs = { "en" }, values = { "Password" })
    String gui_fileinfopanel_linktab_password();

    @Default(lngs = { "en" }, values = { "Please describe your Problem/Bug/Question!" })
    String gui_logger_askQuestion();

    @Default(lngs = { "en" }, values = { "Browse" })
    String gui_btn_select();

    @Default(lngs = { "en" }, values = { "External IP Check Interval [min]" })
    String gui_config_download_ipcheck_externalinterval2();

    @Default(lngs = { "en" }, values = { "Auto rename" })
    String system_download_triggerfileexists_rename();

    @Default(lngs = { "en" }, values = { "Delete from list and disk" })
    String gui_table_contextmenu_deletelistdisk2();

    @Default(lngs = { "en" }, values = { "Progress" })
    String gui_treetable_progress();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all disabled DownloadLinks?" })
    String jd_gui_swing_jdgui_menu_actions_RemoveDisabledAction_message();

    @Default(lngs = { "en" }, values = { "Used Space" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_usedspace();

    @Default(lngs = { "en" }, values = { "Display Threshold" })
    String gui_config_captcha_train_level();

    @Default(lngs = { "en" }, values = { "Set Stopmark" })
    String gui_table_contextmenu_stopmark_set();

    @Default(lngs = { "en" }, values = { "Download complete in" })
    String jd_gui_swing_jdgui_views_info_DownloadInfoPanel_eta();

    @Default(lngs = { "en" }, values = { "Countdown for CAPTCHA window" })
    String gui_config_captcha_train_show_timeout();

    @Default(lngs = { "en" }, values = { "Insert before '%s1'" })
    String gui_table_draganddrop_movepackagebefore(Object s1);

    @Default(lngs = { "en" }, values = { "Do you really want to remove all completed FilePackages?" })
    String jd_gui_swing_jdgui_menu_actions_CleanupPackages_message();

    @Default(lngs = { "en" }, values = { "Disable" })
    String gui_table_contextmenu_disable();

    @Default(lngs = { "en" }, values = { "when package is ready" })
    String gui_config_general_toDoWithDownloads_packageready();

    @Default(lngs = { "en" }, values = { "Backup" })
    String gui_menu_save();

    @Default(lngs = { "en" }, values = { "DELETE" })
    String gui_textcomponent_context_delete_acc();

    @Default(lngs = { "en" }, values = { "Let Reconnects interrupt resumeable downloads" })
    String gui_config_download_autoresume();

    @Default(lngs = { "en" }, values = { "Links" })
    String jd_gui_skins_simple_simplegui_menubar_linksmenu();

    @Default(lngs = { "en" }, values = { "Always select the premium account with the most traffic left for downloading" })
    String jd_gui_swing_jdgui_settings_panels_premium_Premium_accountSelection();

    @Default(lngs = { "en" }, values = { "Socks5" })
    String jd_gui_swing_dialog_ProxyDialog_socks5();

    @Default(lngs = { "en" }, values = { "Force download" })
    String gui_table_contextmenu_tryforce();

    @Default(lngs = { "en" }, values = { "[Plugin disabled]" })
    String gui_downloadlink_plugindisabled();

    @Default(lngs = { "en" }, values = { "File writing" })
    String gui_config_download_write();

    @Default(lngs = { "en" }, values = { "Download stopped" })
    String ballon_download_finished_stopped();

    @Default(lngs = { "en" }, values = { "Host" })
    String gui_treetable_hoster();

    @Default(lngs = { "en" }, values = { "Advanced" })
    String jd_gui_swing_jdgui_settings_panels_gui_advanced_gui_advanced_title();

    @Default(lngs = { "en" }, values = { "ctrl C" })
    String gui_textcomponent_context_copy_acc();

    @Default(lngs = { "en" }, values = { "Comment" })
    String gui_linkgrabber_packagetab_lbl_comment();

    @Default(lngs = { "en" }, values = { "Type:" })
    String jd_gui_swing_dialog_ProxyDialog_type();

    @Default(lngs = { "en" }, values = { "Hotkey" })
    String jd_gui_swing_jdgui_settings_panels_gui_ToolbarController_column_hotkey();

    @Default(lngs = { "en" }, values = { "Warning! JD cannot write to %s1. Check rights!" })
    String installer_nowriteDir_warning(Object s1);

    @Default(lngs = { "en" }, values = { "Linkfilter" })
    String gui_config_gui_linggrabber_ignorelist();

    @Default(lngs = { "en" }, values = { "Priority" })
    String gui_table_contextmenu_priority();

    @Default(lngs = { "en" }, values = { "List of all HTAccess passwords. Each line one password." })
    String plugins_http_htaccess();

    @Default(lngs = { "en" }, values = { "Are you sure that you want to restart JDownloader?" })
    String sys_ask_rlyrestart();

    @Default(lngs = { "en" }, values = { "Password:" })
    String jd_gui_swing_dialog_ProxyDialog_password();

    @Default(lngs = { "en" }, values = { "Autostart downloads in few seconds..." })
    String controller_downloadautostart();

    @Default(lngs = { "en" }, values = { "Language" })
    String gui_config_gui_language();

    @Default(lngs = { "en" }, values = { "filtered Links(s)" })
    String jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_filteredlinks();

    @Default(lngs = { "en" }, values = { "Max. Connections/File" })
    String gui_tooltip_statusbar_max_chunks();

    @Default(lngs = { "en" }, values = { "Highest Priority" })
    String gui_treetable_tooltip_priority3();

    @Default(lngs = { "en" }, values = { "Higher Priority" })
    String gui_treetable_tooltip_priority2();

    @Default(lngs = { "en" }, values = { "High Priority" })
    String gui_treetable_tooltip_priority1();

    @Default(lngs = { "en" }, values = { "Default Priority" })
    String gui_treetable_tooltip_priority0();

    @Default(lngs = { "en" }, values = { "Links(s)" })
    String jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_links();

    @Default(lngs = { "en" }, values = { "Look" })
    String gui_config_gui_view();

    @Default(lngs = { "en" }, values = { "JDownloader License" })
    String jd_gui_swing_components_AboutDialog_license_title();

    @Default(lngs = { "en" }, values = { "Unset Stopmark" })
    String gui_table_contextmenu_stopmark_unset();

    @Default(lngs = { "en" }, values = { "Archive Password(auto)" })
    String gui_linkgrabber_packagetab_lbl_password2();

    @Default(lngs = { "en" }, values = { "Create sub-folders after adding links" })
    String gui_config_general_createsubfoldersbefore();

    @Default(lngs = { "en" }, values = { "Collect, add and select links and URLs" })
    String jd_gui_swing_jdgui_views_linkgrabberview_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Feel" })
    String gui_config_gui_feel();

    @Default(lngs = { "en" }, values = { "Advanced Settings" })
    String jd_gui_swing_jdgui_settings_panels_premium_Premium_settings();

    @Default(lngs = { "en" }, values = { "Keep only selected Hoster" })
    String gui_linkgrabberv2_onlyselectedhoster();

    @Default(lngs = { "en" }, values = { "Delete links from downloadlist and disk?" })
    String gui_downloadlist_delete2();

    @Default(lngs = { "en" }, values = { "Trafficleft" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_trafficleft();

    @Default(lngs = { "en" }, values = { "Maximum of simultaneous downloads per host (0 = no limit)" })
    String gui_config_download_simultan_downloads_per_host();

    @Default(lngs = { "en" }, values = { "Archive Password" })
    String gui_linkgrabber_packagetab_lbl_password();

    @Default(lngs = { "en" }, values = { "shift" })
    String jd_gui_swing_ShortCuts_key_shift();

    @Default(lngs = { "en" }, values = { "Filter" })
    String gui_table_contextmenu_filetype();

    @Default(lngs = { "en" }, values = { "button1" })
    String jd_gui_swing_ShortCuts_key_button1();

    @Default(lngs = { "en" }, values = { "button2" })
    String jd_gui_swing_ShortCuts_key_button2();

    @Default(lngs = { "en" }, values = { "Start Automatically" })
    String gui_taskpanes_download_linkgrabber_config_autostart();

    @Default(lngs = { "en" }, values = { "Loading from" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_loadingFrom();

    @Default(lngs = { "en" }, values = { "button3" })
    String jd_gui_swing_ShortCuts_key_button3();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all offline DownloadLinks?" })
    String jd_gui_swing_jdgui_menu_actions_RemoveOfflineAction_message();

    @Default(lngs = { "en" }, values = { "Do you want to reconnect your internet connection?" })
    String gui_reconnect_confirm();

    @Default(lngs = { "en" }, values = { "Please enter Website for IPCheck here" })
    String gui_config_download_ipcheck_website_default();

    @Default(lngs = { "en" }, values = { "Host" })
    String gui_column_host();

    @Default(lngs = { "en" }, values = { "Archive Password" })
    String gui_fileinfopanel_packagetab_lbl_password();

    @Default(lngs = { "en" }, values = { "Cut" })
    String gui_textcomponent_context_cut();

    @Default(lngs = { "en" }, values = { "Container (RSDF,DLC,CCF,..)" })
    String gui_config_gui_container();

    @Default(lngs = { "en" }, values = { "Hoster %s1" })
    String jd_gui_swing_menu_HosterMenu(Object s1);

    @Default(lngs = { "en" }, values = { "Already on Download List" })
    String gui_linkgrabber_alreadyindl();

    @Default(lngs = { "en" }, values = { "Support board" })
    String jd_gui_swing_components_AboutDialog_forum();

    @Default(lngs = { "en" }, values = { "General Linkgrabber Settings" })
    String gui_config_gui_linggrabber();

    @Default(lngs = { "en" }, values = { "if selected, new links will be added at top of your downloadlist" })
    String gui_tooltips_linkgrabber_topOrBottom();

    @Default(lngs = { "en" }, values = { "Toolbar Manager" })
    String jd_gui_swing_jdgui_settings_panels_gui_ToolbarController_toolbarController_title();

    @Default(lngs = { "en" }, values = { "Check IP online" })
    String gui_config_download_ipcheck_website();

    @Default(lngs = { "en" }, values = { "Status" })
    String gui_fileinfopanel_linktab_status();

    @Default(lngs = { "en" }, values = { "Clean up" })
    String gui_menu_remove();

    @Default(lngs = { "en" }, values = { "%s1 File(s)" })
    String gui_fileinfopanel_packagetab_lbl_files(Object s1);

    @Default(lngs = { "en" }, values = { "Stop" })
    String jd_gui_swing_jdgui_views_downloads_contextmenu_StopAction_name();

    @Default(lngs = { "en" }, values = { "Timeout for ip change [sec]" })
    String reconnect_waitforip();

    @Default(lngs = { "en" }, values = { "Expired" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_expired();

    @Default(lngs = { "en" }, values = { "not checked" })
    String linkgrabber_onlinestatus_unchecked();

    @Default(lngs = { "en" }, values = { "Pass:" })
    String jd_gui_swing_components_AccountDialog_pass();

    @Default(lngs = { "en" }, values = { "No" })
    String gui_btn_no();

    @Default(lngs = { "en" }, values = { "Plugin missing" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_missing();

    @Default(lngs = { "en" }, values = { "No Traffic Left" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_noTrafficLeft();

    @Default(lngs = { "en" }, values = { "Package(s)" })
    String jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_packages();

    @Default(lngs = { "en" }, values = { "Properties" })
    String gui_table_contextmenu_prop();

    @Default(lngs = { "en" }, values = { "User" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_user();

    @Default(lngs = { "en" }, values = { "%s1 Account(s)" })
    String action_premiumview_removeacc_accs(Object s1);

    @Default(lngs = { "en" }, values = { "ctrl X" })
    String gui_textcomponent_context_cut_acc();

    @Default(lngs = { "en" }, values = { "%s1 - %s2 account(s) -- You can download up to %s3 today." })
    String gui_premiumstatus_traffic_tooltip(Object s1, Object s2, Object s3);

    @Default(lngs = { "en" }, values = { "Total size" })
    String jd_gui_swing_jdgui_views_info_LinkGrabberInfoPanel_size();

    @Default(lngs = { "en" }, values = { "About JDownloader" })
    String jd_gui_swing_components_AboutDialog_title();

    @Default(lngs = { "en" }, values = { "Your Reconnect is not configured correct" })
    String gui_menu_action_reconnect_notconfigured_tooltip();

    @Default(lngs = { "en" }, values = { "Upload failed" })
    String gui_logDialog_warning_uploadFailed();

    @Default(lngs = { "en" }, values = { "Name:" })
    String jd_gui_swing_components_AccountDialog_name();

    @Default(lngs = { "en" }, values = { "Captcha settings" })
    String gui_config_captcha_settings();

    @Default(lngs = { "en" }, values = { "Enable Windowdecoration" })
    String gui_config_gui_decoration();

    @Default(lngs = { "en" }, values = { "%s1 offline" })
    String gui_linkgrabber_packageofflinepercent(Object s1);

    @Default(lngs = { "en" }, values = { "Minimize" })
    String ProgressControllerDialog_minimize();

    @Default(lngs = { "en" }, values = { "Download failed" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_failed();

    @Default(lngs = { "en" }, values = { "Links(s)" })
    String jd_gui_swing_jdgui_views_info_DownloadInfoPanel_links();

    @Default(lngs = { "en" }, values = { "Package / Filename" })
    String gui_linkgrabber_header_packagesfiles();

    @Default(lngs = { "en" }, values = { "Accounts" })
    String jd_gui_swing_jdgui_settings_panels_premium_Premium_title2();

    @Default(lngs = { "en" }, values = { "Copy Password" })
    String gui_table_contextmenu_copyPassword();

    @Default(lngs = { "en" }, values = { "Enable Post Processing for this FilePackage, like extracting or merging." })
    String gui_fileinfopanel_packagetab_chb_postProcessing_toolTip();

    @Default(lngs = { "en" }, values = { "Plugin error" })
    String gui_treetable_error_plugin();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all duplicated DownloadLinks?" })
    String jd_gui_swing_jdgui_menu_actions_RemoveDupesAction_message();

    @Default(lngs = { "en" }, values = { "Continue with selected package(s)" })
    String gui_linkgrabberv2_lg_continueselected();

    @Default(lngs = { "en" }, values = { "Activate" })
    String gui_column_status();

    @Default(lngs = { "en" }, values = { "Add new Account" })
    String jd_gui_swing_components_AccountDialog_title();

    @Default(lngs = { "en" }, values = { "Package" })
    String gui_fileinfopanel_packagetab();

    @Default(lngs = { "en" }, values = { "Max. Dls." })
    String gui_statusbar_sim_ownloads();

    @Default(lngs = { "en" }, values = { "More" })
    String gui_table_contextmenu_more();

    @Default(lngs = { "en" }, values = { "Pause downloads. Limits global speed to %s1 KiB/s" })
    String gui_menu_action_break2_desc(Object s1);

    @Default(lngs = { "en" }, values = { ".*(error|failed).*" })
    String userio_errorregex();

    @Default(lngs = { "en" }, values = { "ETA: %s1" })
    String gui_fileinfopanel_linktab_eta2(Object s1);

    @Default(lngs = { "en" }, values = { "Auto reconnect. Get a new IP by resetting your internet connection" })
    String gui_menu_action_reconnectauto_desc();

    @Default(lngs = { "en" }, values = { "Plugins" })
    String jd_gui_swing_jdgui_settings_panels_ConfigPanelPlugin_plugins_title();

    @Default(lngs = { "en" }, values = { "SFV/CRC check when possible" })
    String gui_config_download_crc();

    @Default(lngs = { "en" }, values = { "Continue with selected link(s)" })
    String gui_linkgrabberv2_lg_continueselectedlinks();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String gui_linkgrabber_packagetab_lbl_name();

    @Default(lngs = { "en" }, values = { "meta" })
    String jd_gui_swing_ShortCuts_key_meta();

    @Default(lngs = { "en" }, values = { "Cash" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_cash();

    @Default(lngs = { "en" }, values = { "Do not start new links if reconnect requested" })
    String gui_config_download_preferreconnect();

    @Default(lngs = { "en" }, values = { "Download started" })
    String ballon_download_finished_started();

    @Default(lngs = { "en" }, values = { "if selected, links will get added and started automatically" })
    String gui_tooltips_linkgrabber_autostart();

    @Default(lngs = { "en" }, values = { "Upload Log" })
    String jd_gui_swing_jdgui_views_info_LogInfoPanel_upload();

    @Default(lngs = { "en" }, values = { "Dialog Information has been reseted." })
    String gui_config_gui_resetdialogs_message();

    @Default(lngs = { "en" }, values = { "Expanded" })
    String jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages_expanded();

    @Default(lngs = { "en" }, values = { "Please enter!" })
    String jd_gui_userio_defaulttitle_input();

    @Default(lngs = { "en" }, values = { "Reset" })
    String gui_table_contextmenu_reset();

    @Default(lngs = { "en" }, values = { "Enabled" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_enabled();

    @Default(lngs = { "en" }, values = { "If the file already exists:" })
    String system_download_triggerfileexists();

    @Default(lngs = { "en" }, values = { "Download & Network" })
    String jd_gui_swing_jdgui_settings_panels_downloadandnetwork_general_download_title();

    @Default(lngs = { "en" }, values = { "Set download password" })
    String gui_table_contextmenu_setdlpw();

    @Default(lngs = { "en" }, values = { "Collapsed" })
    String jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_newpackages_collapsed();

    @Default(lngs = { "en" }, values = { "Progress" })
    String jd_gui_swing_jdgui_views_info_DownloadInfoPanel_progress();

    @Default(lngs = { "en" }, values = { "Name" })
    String jd_gui_swing_jdgui_settings_panels_gui_ToolbarController_column_name();

    @Default(lngs = { "en" }, values = { "Downloadlist and Progress" })
    String jd_gui_swing_jdgui_views_downloadview_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Extensions" })
    String jd_gui_swing_jdgui_settings_panels_ConfigPanelAddons_addons_title();

    @Default(lngs = { "en" }, values = { "Package / Filename" })
    String gui_treetable_name();

    @Default(lngs = { "en" }, values = { "Buy Premium" })
    String jd_gui_swing_jdgui_actions_ActionController_buy_title();

    @Default(lngs = { "en" }, values = { "ctrl A" })
    String gui_textcomponent_context_selectall_acc();

    @Default(lngs = { "en" }, values = { "Max repeats (-1 = no limit)" })
    String reconnect_retries();

    @Default(lngs = { "en" }, values = { "Linkgrabber" })
    String jd_gui_swing_jdgui_views_linkgrabberview_tab_title();

    @Default(lngs = { "en" }, values = { "%s1 - %s2 account(s) -- At the moment it may be that no premium traffic is left." })
    String gui_premiumstatus_expired_maybetraffic_tooltip(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "Stopmark is set" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_stopmark();

    @Default(lngs = { "en" }, values = { "After Installation, JDownloader will update to the latest version." })
    String installer_gui_message();

    @Default(lngs = { "en" }, values = { "Close Tab" })
    String jd_gui_swing_components_JDCloseAction_closeTab();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all completed DownloadLinks?" })
    String jd_gui_swing_jdgui_menu_actions_CleanupDownload_message();

    @Default(lngs = { "en" }, values = { "ctrl V" })
    String gui_textcomponent_context_paste_acc();

    @Default(lngs = { "en" }, values = { "%s1 - %s2 account(s) -- At the moment no premium traffic is available." })
    String gui_premiumstatus_expired_traffic_tooltip(Object s1, Object s2);

    @Default(lngs = { "en" }, values = { "Check Online Status" })
    String gui_table_contextmenu_check();

    @Default(lngs = { "en" }, values = { "Reset" })
    String gui_config_gui_resetdialogs_short();

    @Default(lngs = { "en" }, values = { "User Interface" })
    String jd_gui_swing_jdgui_settings_panels_gui_General_gui_title();

    @Default(lngs = { "en" }, values = { "Unknown FileSize" })
    String jd_gui_swing_jdgui_views_downloadview_Columns_ProgressColumn_unknownFilesize();

    @Default(lngs = { "en" }, values = { "never" })
    String gui_config_general_toDoWithDownloads_never();

    @Default(lngs = { "en" }, values = { "Settings" })
    String gui_column_settings();

    @Default(lngs = { "en" }, values = { "Reload Download Container" })
    String gui_config_reloadcontainer();

    @Default(lngs = { "en" }, values = { "Change Columns" })
    String jd_gui_swing_components_table_JDTable_columnControl();

    @Default(lngs = { "en" }, values = { "Remaining" })
    String gui_treetable_remaining();

    @Default(lngs = { "en" }, values = { "IP:" })
    String jd_gui_swing_dialog_ProxyDialog_hostip();

    @Default(lngs = { "en" }, values = { "Sort Packages" })
    String gui_table_contextmenu_packagesort();

    @Default(lngs = { "en" }, values = { "Download" })
    String jd_gui_swing_jdgui_views_downloadview_tab_title();

    @Default(lngs = { "en" }, values = { "ctrl" })
    String jd_gui_swing_ShortCuts_key_ctrl();

    @Default(lngs = { "en" }, values = { "Reconnection IP-Check" })
    String gui_config_download_ipcheck();

    @Default(lngs = { "en" }, values = { "Save to" })
    String gui_fileinfopanel_linktab_saveto();

    @Default(lngs = { "en" }, values = { "Settings" })
    String gui_btn_settings();

    @Default(lngs = { "en" }, values = { "Maximum simultaneous Downloads [1..20]" })
    String gui_tooltip_statusbar_simultan_downloads();

    @Default(lngs = { "en" }, values = { "%s1 links" })
    String gui_downloadlist_delete_size_packagev2(Object s1);

    @Default(lngs = { "en" }, values = { "Password" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_pass();

    @Default(lngs = { "en" }, values = { "Check linkinfo and onlinestatus" })
    String gui_config_linkgrabber_onlincheck();

    @Default(lngs = { "en" }, values = { "Yes" })
    String gui_btn_yes();

    @Default(lngs = { "en" }, values = { "Open in browser" })
    String gui_table_contextmenu_browselink();

    @Default(lngs = { "en" }, values = { "Contributors" })
    String jd_gui_swing_components_AboutDialog_contributers();

    @Default(lngs = { "en" }, values = { "Stopping current downloads..." })
    String gui_downloadstop();

    @Default(lngs = { "en" }, values = { "Use" })
    String gui_column_use();

    @Default(lngs = { "en" }, values = { "Password" })
    String gui_column_pass();

    @Default(lngs = { "en" }, values = { "Linkgrabber" })
    String jd_gui_swing_jdgui_settings_panels_gui_Linkgrabber_gui_linkgrabber_title();

    @Default(lngs = { "en" }, values = { "Add new Proxy" })
    String jd_gui_swing_dialog_ProxyDialog_title();

    @Default(lngs = { "en" }, values = { "Linkname" })
    String gui_fileinfopanel_linktab_name();

    @Default(lngs = { "en" }, values = { "temp. uncheckable" })
    String linkgrabber_onlinestatus_uncheckable();

    @Default(lngs = { "en" }, values = { "Download Control" })
    String gui_config_download_download_tab();

    @Default(lngs = { "en" }, values = { "Settings" })
    String jd_gui_swing_jdgui_views_configurationview_tab_title();

    @Default(lngs = { "en" }, values = { "Max. Buffersize[KB]" })
    String gui_config_download_buffersize2();

    @Default(lngs = { "en" }, values = { "Download" })
    String ballon_download_title();

    @Default(lngs = { "en" }, values = { "Description" })
    String jd_gui_swing_jdgui_settings_panels_gui_ToolbarController_column_desc();

    @Default(lngs = { "en" }, values = { "Is selected, download starts after adding new links" })
    String gui_tooltips_linkgrabber_startlinksafteradd();

    @Default(lngs = { "en" }, values = { "General Reconnect Settings" })
    String gui_config_reconnect_shared();

    @Default(lngs = { "en" }, values = { "DefaultProxy" })
    String gui_column_defaultproxy();

    @Default(lngs = { "en" }, values = { "Remove selected?" })
    String action_premiumview_removeacc_ask();

    @Default(lngs = { "en" }, values = { "Close %s1" })
    String jd_gui_swing_components_JDCollapser_closetooltip(Object s1);

    @Default(lngs = { "en" }, values = { "Enable" })
    String gui_table_contextmenu_enable();

    @Default(lngs = { "en" }, values = { "Add URL(s)" })
    String gui_dialog_addurl_title();

    @Default(lngs = { "en" }, values = { "Which hoster are you interested in?" })
    String jd_gui_swing_jdgui_actions_ActionController_buy_message();

    @Default(lngs = { "en" }, values = { "Disable automatic CAPTCHA" })
    String gui_config_captcha_jac_disable();

    @Default(lngs = { "en" }, values = { "Finished date" })
    String gui_treetable_finished();

    @Default(lngs = { "en" }, values = { "action.premium.buy" })
    String gui_menu_action_premium_buy_name();

    @Default(lngs = { "en" }, values = { "Do you really want to disable all premium accounts?" })
    String dialogs_premiumstatus_global_message();

    @Default(lngs = { "en" }, values = { "JAntiCaptcha" })
    String jd_gui_swing_jdgui_settings_panels_ConfigPanelCaptcha_captcha_title();

    @Default(lngs = { "en" }, values = { "Unknown" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_unknown();

    @Default(lngs = { "en" }, values = { "Linklist Backup failed! Check %s1 for rights!" })
    String gui_backup_finished_failed(Object s1);

    @Default(lngs = { "en" }, values = { "Manual reconnect. Get a new IP by resetting your internet connection" })
    String gui_menu_action_reconnectman_desc();

    @Default(lngs = { "en" }, values = { "Insert at the beginning of Package '%s1'" })
    String gui_table_draganddrop_insertinpackagestart(Object s1);

    @Default(lngs = { "en" }, values = { "Name of the new package" })
    String gui_linklist_newpackage_message();

    @Default(lngs = { "en" }, values = { "Status" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_status();

    @Default(lngs = { "en" }, values = { "Please enter!" })
    String userio_input_title();

    @Default(lngs = { "en" }, values = { "Reconnection" })
    String jd_gui_swing_jdgui_settings_panels_reconnect_Advanced_reconnect_advanced_title();

    @Default(lngs = { "en" }, values = { "Username" })
    String gui_column_user();

    @Default(lngs = { "en" }, values = { "Read TOS" })
    String jd_gui_swing_jdgui_settings_panels_hoster_columns_TosColumn_read();

    @Default(lngs = { "en" }, values = { "Invalid Account" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_invalidAccount();

    @Default(lngs = { "en" }, values = { "HTAccess logins" })
    String jd_gui_swing_jdgui_settings_panels_passwords_PasswordListHTAccess_general_title();

    @Default(lngs = { "en" }, values = { "First IP check wait time (sec)" })
    String reconnect_waittimetofirstipcheck();

    @Default(lngs = { "en" }, values = { "Account" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_account();

    @Default(lngs = { "en" }, values = { "Host/Port:" })
    String jd_gui_swing_dialog_ProxyDialog_hostport();

    @Default(lngs = { "en" }, values = { "Continue" })
    String jd_gui_swing_jdgui_actions_ActionController_continue();

    @Default(lngs = { "en" }, values = { "Open Directory" })
    String gui_table_contextmenu_downloaddir();

    @Default(lngs = { "en" }, values = { "Click here to close this Balloon and open JD" })
    String jd_gui_swing_components_Balloon_toolTip();

    @Default(lngs = { "en" }, values = { "Message" })
    String gui_dialogs_message_title();

    @Default(lngs = { "en" }, values = { "Speed of pause in KiB/s" })
    String gui_config_download_pausespeed();

    @Default(lngs = { "en" }, values = { "immediately" })
    String gui_config_general_toDoWithDownloads_immediate();

    @Default(lngs = { "en" }, values = { "Unlimited" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumTableRenderer_unlimited();

    @Default(lngs = { "en" }, values = { "Proxy" })
    String gui_treetable_proxy();

    @Default(lngs = { "en" }, values = { "Number of Files" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_filesnum();

    @Default(lngs = { "en" }, values = { "Cancel" })
    String gui_btn_cancel();

    @Default(lngs = { "en" }, values = { "Size" })
    String gui_treetable_header_size();

    @Default(lngs = { "en" }, values = { "About" })
    String gui_menu_about();

    @Default(lngs = { "en" }, values = { "Log" })
    String jd_gui_swing_jdgui_views_log_tab_title();

    @Default(lngs = { "en" }, values = { "Low Priority" })
    String gui_treetable_tooltip_priority_1();

    @Default(lngs = { "en" }, values = { "at startup" })
    String gui_config_general_toDoWithDownloads_atstart();

    @Default(lngs = { "en" }, values = { "Overwrite" })
    String system_download_triggerfileexists_overwrite();

    @Default(lngs = { "en" }, values = { "Add a URL(s). JDownloader will load and parse them for further links." })
    String gui_dialog_addurl_message();

    @Default(lngs = { "en" }, values = { "Loaded" })
    String gui_treetable_loaded();

    @Default(lngs = { "en" }, values = { "Add links" })
    String gui_menu_addlinks();

    @Default(lngs = { "en" }, values = { "Enable Click'n'Load Support" })
    String gui_config_linkgrabber_cnl2();

    @Default(lngs = { "en" }, values = { "Delete" })
    String gui_textcomponent_context_delete();

    @Default(lngs = { "en" }, values = { "Backup" })
    String gui_balloon_backup_title();

    @Default(lngs = { "en" }, values = { "Delete selected links?" })
    String gui_downloadlist_delete();

    @Default(lngs = { "en" }, values = { "Hoster:" })
    String jd_gui_swing_components_AccountDialog_hoster();

    @Default(lngs = { "en" }, values = { "Move into new Package" })
    String gui_table_contextmenu_newpackage();

    @Default(lngs = { "en" }, values = { "Username:" })
    String jd_gui_swing_dialog_ProxyDialog_username();

    @Default(lngs = { "en" }, values = { "Ask for each file" })
    String system_download_triggerfileexists_ask();

    @Default(lngs = { "en" }, values = { "Warning! JD is installed in %s1. This causes errors." })
    String installer_vistaDir_warning(Object s1);

    @Default(lngs = { "en" }, values = { "Version: %s1" })
    String jd_gui_swing_jdgui_settings_panels_ConfigPanelAddons_version(Object s1);

    @Default(lngs = { "en" }, values = { "Archive Password(auto)" })
    String gui_fileinfopanel_packagetab_lbl_password2();

    @Default(lngs = { "en" }, values = { "IP-Check disabled!" })
    String jd_gui_swing_jdgui_settings_panels_downloadandnetwork_advanced_ipcheckdisable_warning_title();

    @Default(lngs = { "en" }, values = { "Aborted" })
    String gui_linkgrabber_aborted();

    @Default(lngs = { "en" }, values = { "Save to" })
    String gui_linkgrabber_packagetab_lbl_saveto();

    @Default(lngs = { "en" }, values = { "Speed Limit (KiB/s) [0 = Infinite]" })
    String gui_tooltip_statusbar_speedlimiter();

    @Default(lngs = { "en" }, values = { "Reset selected downloads?" })
    String gui_downloadlist_reset();

    @Default(lngs = { "en" }, values = { "Ask for each package" })
    String system_download_triggerfileexists_askpackage();

    @Default(lngs = { "en" }, values = { "Performance" })
    String gui_config_gui_performance();

    @Default(lngs = { "en" }, values = { "Linklist Backup successful! (%s1)" })
    String gui_backup_finished_success(Object s1);

    @Default(lngs = { "en" }, values = { "Are you sure that you want to exit JDownloader?" })
    String sys_ask_rlyclose();

    @Default(lngs = { "en" }, values = { "Show infopanel on linkgrab" })
    String gui_config_linkgrabber_infopanel_onlinkgrab();

    @Default(lngs = { "en" }, values = { "Set download password" })
    String gui_linklist_setpw_message();

    @Default(lngs = { "en" }, values = { "Download finished" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_finished();

    @Default(lngs = { "en" }, values = { "Please enter..." })
    String gui_captchaWindow_askForInput();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String gui_fileinfopanel_packagetab_lbl_name();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all failed DownloadLinks?" })
    String jd_gui_swing_jdgui_menu_actions_RemoveFailedAction_message();

    @Default(lngs = { "en" }, values = { "TrafficShare" })
    String jd_gui_swing_jdgui_settings_panels_premium_PremiumJTableModel_trafficshare();

    @Default(lngs = { "en" }, values = { "Upload of logfile failed!" })
    String sys_warning_loguploadfailed();

    @Default(lngs = { "en" }, values = { "Size of Captcha in percent:" })
    String jd_gui_swing_jdgui_settings_panels_ConfigPanelCaptcha_captchaSize();

    @Default(lngs = { "en" }, values = { "Parse URL(s)" })
    String gui_dialog_addurl_okoption_parse();

    @Default(lngs = { "en" }, values = { "The linkfilter is used to filter links based on regular expressions." })
    String gui_config_linkgrabber_ignorelist();

    @Default(lngs = { "en" }, values = { "List of all passwords. Each line one password." })
    String plugins_optional_jdunrar_config_passwordlist();

    @Default(lngs = { "en" }, values = { "Chunks" })
    String gui_fileinfopanel_linktab_chunks();

    @Default(lngs = { "en" }, values = { "Drop after '%s1'" })
    String gui_table_draganddrop_after(Object s1);

    @Default(lngs = { "en" }, values = { "Homepage" })
    String jd_gui_swing_components_AboutDialog_homepage();

    @Default(lngs = { "en" }, values = { "Start after adding" })
    String gui_taskpanes_download_linkgrabber_config_startofter();

    @Default(lngs = { "en" }, values = { "Copy" })
    String gui_textcomponent_context_copy();

    @Default(lngs = { "en" }, values = { "online" })
    String linkgrabber_onlinestatus_online();

    @Default(lngs = { "en" }, values = { "Extracting" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_extract();

    @Default(lngs = { "en" }, values = { "TOS" })
    String gui_column_tos();

    @Default(lngs = { "en" }, values = { "URL" })
    String gui_fileinfopanel_linktab_url();

    @Default(lngs = { "en" }, values = { "Disable IP-Check" })
    String gui_config_download_ipcheck_disable();

    @Default(lngs = { "en" }, values = { "Download directory" })
    String gui_config_general_downloaddirectory();

    @Default(lngs = { "en" }, values = { "Clear linkgrabber list?" })
    String gui_linkgrabberv2_lg_clear_ask();

    @Default(lngs = { "en" }, values = { "Resume" })
    String gui_table_contextmenu_resume();

    @Default(lngs = { "en" }, values = { "Allowed IPs" })
    String gui_config_download_ipcheck_mask();

    @Default(lngs = { "en" }, values = { "This will restart JDownloader and do a FULL-Update. Continue?" })
    String sys_ask_rlyrestore();

    @Default(lngs = { "en" }, values = { "Post Processing" })
    String gui_fileinfopanel_packagetab_chb_postProcessing();

    @Default(lngs = { "en" }, values = { "Blacklist (Ignore the following links. Allow all others)" })
    String settings_linkgrabber_filter_blackorwhite_black();

    @Default(lngs = { "en" }, values = { "Whitelist (Allow the following links. Ignore all others)" })
    String settings_linkgrabber_filter_blackorwhite_white();

    @Default(lngs = { "en" }, values = { "Filter Type" })
    String gui_config_linkgrabber_filter_type();

    @Default(lngs = { "en" }, values = { "Delete" })
    String settings_linkgrabber_filter_action_remove();

    @Default(lngs = { "en" }, values = { "Add" })
    String settings_linkgrabber_filter_action_add();

    @Default(lngs = { "en" }, values = { "Run Test" })
    String settings_linkgrabber_filter_action_test();

    @Default(lngs = { "en" }, values = { "Enable selected" })
    String settings_linkgrabber_filter_action_enable();

    @Default(lngs = { "en" }, values = { "Disable selected" })
    String settings_linkgrabber_filter_action_disable();

    @Default(lngs = { "en" }, values = { "Disable all" })
    String settings_linkgrabber_filter_action_all();

    @Default(lngs = { "en" }, values = { "Enable all" })
    String settings_linkgrabber_filter_action_enable_all();

    @Default(lngs = { "en" }, values = { "Miscellaneous" })
    String gui_config_various();

    @Default(lngs = { "en" }, values = { "Auto open Link Containers (dlc,ccf,...)" })
    String gui_config_simple_container();

    @Default(lngs = { "en" }, values = { "Size of Captcha Dialogs" })
    String gui_config_barrierfree_captchasize();
}