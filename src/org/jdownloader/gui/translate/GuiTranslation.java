package org.jdownloader.gui.translate;

import jd.controlling.reconnect.ipcheck.IP;

import org.appwork.txtresource.Default;
import org.appwork.txtresource.Defaults;
import org.appwork.txtresource.DescriptionForTranslationEntry;
import org.appwork.txtresource.TranslateInterface;
import org.appwork.utils.net.httpconnection.HTTPProxy;

@Defaults(lngs = { "en" })
public interface GuiTranslation extends TranslateInterface {

    @Default(lngs = { "en" }, values = { "About JDownloader" })
    String action_aboutaction();

    @Default(lngs = { "en" }, values = { "About JDownloader" })
    String action_aboutaction_tooltip();

    @Default(lngs = { "en" }, values = { "Add a new Premium Account" })
    String action_add_premium_account_tooltip();

    @Default(lngs = { "en" }, values = { "Add Container" })
    String action_addcontainer();

    String action_addcontainer_tooltip();

    @Default(lngs = { "en" }, values = { "Changelog" })
    String action_changelog();

    String action_changelog_tooltip();

    @Default(lngs = { "en" }, values = { "Enable or Disable Clipboard Observer." })
    String action_clipboard_observer_tooltip();

    @Default(lngs = { "en" }, values = { "Exit" })
    String action_exit();

    String action_exit_tooltip();

    @Default(lngs = { "en" }, values = { "Help" })
    String action_help();

    String action_help_tooltip();

    @Default(lngs = { "en" }, values = { "Open the default download destination" })
    String action_open_dlfolder_tooltip();

    @Default(lngs = { "en" }, values = { "Enable or disable all Premium Accounts" })
    String action_premium_toggle_tooltip();

    @Default(lngs = { "en" }, values = { "Perform a Reconnect, to get a new dynamic IP" })
    String action_reconnect_invoke_tooltip();

    @Default(lngs = { "en" }, values = { "Enable or Disable Auto-Reconnection" })
    String action_reconnect_toggle_tooltip();

    // action_//
    @Default(lngs = { "en" }, values = { "Restart" })
    String action_restart();

    @Default(lngs = { "en" }, values = { "Restart JDownloader" })
    String action_restart_tooltip();

    // action_//
    @Default(lngs = { "en" }, values = { "Settings" })
    String action_settings_menu();

    @Default(lngs = { "en" }, values = { "Open Settings panel" })
    String action_settings_menu_tooltip();

    @Default(lngs = { "en" }, values = { "Check for new updates." })
    String action_start_update_tooltip();

    @Default(lngs = { "en" }, values = { "Stops all running Downloads" })
    String action_stop_downloads_tooltip();

    @Default(lngs = { "en" }, values = { "Use Logins" })
    String authtablemodel_column_enabled();

    @Default(lngs = { "en" }, values = { "Host/URL" })
    String authtablemodel_column_host();

    @Default(lngs = { "en" }, values = { "Password" })
    String authtablemodel_column_password();

    @Default(lngs = { "en" }, values = { "Servertype" })
    String authtablemodel_column_type();

    @Default(lngs = { "en" }, values = { "ftp://" })
    String authtablemodel_column_type_ftp();

    @Default(lngs = { "en" }, values = { "http://" })
    String authtablemodel_column_type_http();

    @Default(lngs = { "en" }, values = { "Username" })
    String authtablemodel_column_username();

    @Default(lngs = { "en" }, values = { "Extension Modules" })
    String extensionManager_title();

    @Default(lngs = { "en" }, values = { "Browse" })
    String gui_btn_select();

    @Default(lngs = { "en" }, values = { "Enter Captcha for %s1 to continue!" })
    String gui_captchaWindow_askForInput(String hoster);

    @Default(lngs = { "en" }, values = { "DefaultProxy" })
    String gui_column_defaultproxy();

    @Default(lngs = { "en" }, values = { "Host/IP" })
    String gui_column_host();

    @Default(lngs = { "en" }, values = { "Password" })
    String gui_column_pass();

    @Default(lngs = { "en" }, values = { "Port" })
    String gui_column_port();

    @Default(lngs = { "en" }, values = { "Proxytype" })
    String gui_column_proxytype();

    @Default(lngs = { "en" }, values = { "Native" })
    String gui_column_native();

    @Default(lngs = { "en" }, values = { "Use" })
    String gui_column_use();

    @Default(lngs = { "en" }, values = { "Username" })
    String gui_column_user();

    @Default(lngs = { "en" }, values = { "Size of Captcha Dialogs" })
    String gui_config_barrierfree_captchasize();

    @Default(lngs = { "en" }, values = { "SFV/CRC check when possible" })
    String gui_config_download_crc();

    @Default(lngs = { "en" }, values = { "Maximum of simultaneous downloads per host" })
    String gui_config_download_simultan_downloads_per_host2();

    @Default(lngs = { "en" }, values = { "File writing" })
    String gui_config_download_write();

    @Default(lngs = { "en" }, values = { "Download directory" })
    String gui_config_general_downloaddirectory();

    @Default(lngs = { "en" }, values = { "Remove finished downloads ..." })
    String gui_config_general_todowithdownloads();

    @Default(lngs = { "en" }, values = { "at startup" })
    String gui_config_general_toDoWithDownloads_atstart();

    @Default(lngs = { "en" }, values = { "immediately" })
    String gui_config_general_toDoWithDownloads_immediate();

    @Default(lngs = { "en" }, values = { "never" })
    String gui_config_general_toDoWithDownloads_never();

    @Default(lngs = { "en" }, values = { "when package is ready" })
    String gui_config_general_toDoWithDownloads_packageready();

    @Default(lngs = { "en" }, values = { "Auto open Link Containers (dlc,ccf,...)" })
    String gui_config_simple_container();

    @Default(lngs = { "en" }, values = { "Miscellaneous" })
    String gui_config_various();

    @Default(lngs = { "en" }, values = { "Message" })
    String gui_dialogs_message_title();

    @Default(lngs = { "en" }, values = { "%s1 links" })
    String gui_downloadlist_delete_size_packagev2(Object s1);

    @Default(lngs = { "en" }, values = { "Load DLC file" })
    String gui_filechooser_loaddlc();

    @Default(lngs = { "en" }, values = { "New Package Name" })
    String gui_linklist_editpackagename_message();

    @Default(lngs = { "en" }, values = { "Help" })
    String gui_menu_about();

    @Default(lngs = { "en" }, values = { "Pause downloads. Limits global speed to %s1 KiB/s" })
    String gui_menu_action_break2_desc(Object s1);

    @Default(lngs = { "en" }, values = { "action.premium.buy" })
    String gui_menu_action_premium_buy_name();

    @Default(lngs = { "en" }, values = { "Do you want to reconnect your internet connection?" })
    String gui_reconnect_confirm();

    @Default(lngs = { "en" }, values = { "Advanced Settings" })
    String gui_settings_advanced_title();

    @Default(lngs = { "en" }, values = { "Open in Browser" })
    String gui_table_contextmenu_browselink();

    @Default(lngs = { "en" }, values = { "Check Online Status" })
    String gui_table_contextmenu_check();

    @Default(lngs = { "en" }, values = { "Delete from list" })
    String gui_table_contextmenu_deletelist2();

    @Default(lngs = { "en" }, values = { "Create DLC" })
    String gui_table_contextmenu_dlc();

    @Default(lngs = { "en" }, values = { "Open Directory" })
    String gui_table_contextmenu_downloaddir();

    @Default(lngs = { "en" }, values = { "Change Package Name" })
    String gui_table_contextmenu_editpackagename();

    @Default(lngs = { "en" }, values = { "Move into new Package" })
    String gui_table_contextmenu_newpackage();

    @Default(lngs = { "en" }, values = { "Open File" })
    String gui_table_contextmenu_openfile();

    @Default(lngs = { "en" }, values = { "Reset" })
    String gui_table_contextmenu_reset();

    @Default(lngs = { "en" }, values = { "Resume" })
    String gui_table_contextmenu_resume();

    @Default(lngs = { "en" }, values = { "Stop after this download" })
    String gui_table_contextmenu_stopmark_set();

    @Default(lngs = { "en" }, values = { "Remove Stopmark" })
    String gui_table_contextmenu_stopmark_unset();

    @Default(lngs = { "en" }, values = { "Watch As you download" })
    String gui_table_contextmenu_watch_as_you_download();

    @Default(lngs = { "en" }, values = { "Plugin error" })
    String gui_treetable_error_plugin();

    @Default(lngs = { "en" }, values = { "Low Priority" })
    String gui_treetable_tooltip_priority_1();

    @Default(lngs = { "en" }, values = { "Default Priority" })
    String gui_treetable_tooltip_priority0();

    @Default(lngs = { "en" }, values = { "High Priority" })
    String gui_treetable_tooltip_priority1();

    @Default(lngs = { "en" }, values = { "Higher Priority" })
    String gui_treetable_tooltip_priority2();

    @Default(lngs = { "en" }, values = { "Highest Priority" })
    String gui_treetable_tooltip_priority3();

    @Default(lngs = { "en" }, values = { "File" })
    String jd_gui_skins_simple_simplegui_menubar_filemenu();

    @Default(lngs = { "en" }, values = { "Contributors" })
    String jd_gui_swing_components_AboutDialog_contributers();

    @Default(lngs = { "en" }, values = { "Support board" })
    String jd_gui_swing_components_AboutDialog_forum();

    @Default(lngs = { "en" }, values = { "Homepage" })
    String jd_gui_swing_components_AboutDialog_homepage();

    @Default(lngs = { "en" }, values = { "Show license" })
    String jd_gui_swing_components_AboutDialog_license();

    @Default(lngs = { "en" }, values = { "JDownloader License" })
    String jd_gui_swing_components_AboutDialog_license_title();

    @Default(lngs = { "en" }, values = { "About JDownloader" })
    String jd_gui_swing_components_AboutDialog_title();

    @Default(lngs = { "en" }, values = { "Name:" })
    String jd_gui_swing_components_AccountDialog_name();

    @Default(lngs = { "en" }, values = { "Pass:" })
    String jd_gui_swing_components_AccountDialog_pass();

    @Default(lngs = { "en" }, values = { "Add new Account" })
    String jd_gui_swing_components_AccountDialog_title();

    @Default(lngs = { "en" }, values = { "Host/Port:" })
    String jd_gui_swing_dialog_ProxyDialog_hostport();

    @Default(lngs = { "en" }, values = { "HTTP" })
    String jd_gui_swing_dialog_ProxyDialog_http();

    @Default(lngs = { "en" }, values = { "Password:" })
    String jd_gui_swing_dialog_ProxyDialog_password();

    @Default(lngs = { "en" }, values = { "Socks5" })
    String jd_gui_swing_dialog_ProxyDialog_socks5();

    @Default(lngs = { "en" }, values = { "Socks4" })
    String jd_gui_swing_dialog_ProxyDialog_socks4();

    @Default(lngs = { "en" }, values = { "Direct" })
    String jd_gui_swing_dialog_ProxyDialog_direct();

    @Default(lngs = { "en" }, values = { "Add new Proxy" })
    String jd_gui_swing_dialog_ProxyDialog_title();

    @Default(lngs = { "en" }, values = { "Type:" })
    String jd_gui_swing_dialog_ProxyDialog_type();

    @Default(lngs = { "en" }, values = { "Username:" })
    String jd_gui_swing_dialog_ProxyDialog_username();

    @Default(lngs = { "en" }, values = { "This option needs a JDownloader restart." })
    String jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion();

    @Default(lngs = { "en" }, values = { "Restart NOW!" })
    String jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_ok();

    @Default(lngs = { "en" }, values = { "Restart required!" })
    String jd_gui_swing_jdgui_settings_ConfigPanel_restartquestion_title();

    @Default(lngs = { "en" }, values = { "Settings" })
    String jd_gui_swing_jdgui_views_configurationview_tab_title();

    @Default(lngs = { "en" }, values = { "All options and settings for JDownloader" })
    String jd_gui_swing_jdgui_views_configurationview_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Download" })
    String jd_gui_swing_jdgui_views_downloadview_tab_title();

    @Default(lngs = { "en" }, values = { "Downloadlist and Progress" })
    String jd_gui_swing_jdgui_views_downloadview_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Stopmark is set" })
    String jd_gui_swing_jdgui_views_downloadview_TableRenderer_stopmark();

    @Default(lngs = { "en" }, values = { "Linkgrabber" })
    String jd_gui_swing_jdgui_views_linkgrabberview_tab_title();

    @Default(lngs = { "en" }, values = { "Collect, add and select links and URLs" })
    String jd_gui_swing_jdgui_views_linkgrabberview_tab_tooltip();

    @Default(lngs = { "en" }, values = { "Please confirm!" })
    String jd_gui_userio_defaulttitle_confirm();

    @Default(lngs = { "en" }, values = { "Please enter!" })
    String jd_gui_userio_defaulttitle_input();

    @Default(lngs = { "en" }, values = { "offline" })
    String linkgrabber_onlinestatus_offline();

    @Default(lngs = { "en" }, values = { "online" })
    String linkgrabber_onlinestatus_online();

    @Default(lngs = { "en" }, values = { "temp. uncheckable" })
    String linkgrabber_onlinestatus_uncheckable();

    @Default(lngs = { "en" }, values = { "not checked" })
    String linkgrabber_onlinestatus_unchecked();

    @Default(lngs = { "en" }, values = { "dd.MM.yy HH:mm" })
    String added_date_column_dateformat();

    @Default(lngs = { "en" }, values = { "Actions" })
    String premiumaccounttablemodel_column_actions();

    @Default(lngs = { "en" }, values = { "Enabled" })
    String premiumaccounttablemodel_column_enabled();

    @Default(lngs = { "en" }, values = { "Expire Date" })
    String premiumaccounttablemodel_column_expiredate();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String premiumaccounttablemodel_column_hoster();

    @Default(lngs = { "en" }, values = { "Password" })
    String premiumaccounttablemodel_column_password();

    @Default(lngs = { "en" }, values = { "Download Traffic left" })
    String premiumaccounttablemodel_column_trafficleft();

    @Default(lngs = { "en" }, values = { "Unlimited" })
    String premiumaccounttablemodel_column_trafficleft_unlimited();

    @Default(lngs = { "en" }, values = { "Temp. disabled %s1" })
    String premiumaccounttablemodel_column_trafficleft_tempdisabled(String s);

    @Default(lngs = { "en" }, values = { "Username" })
    String premiumaccounttablemodel_column_user();

    @Default(lngs = { "en" }, values = { "Buy" })
    String settings_accountmanager_buy();

    @Default(lngs = { "en" }, values = { "Account Information" })
    String settings_accountmanager_info();

    @Default(lngs = { "en" }, values = { "Premiumzone" })
    String settings_accountmanager_premiumzone();

    @Default(lngs = { "en" }, values = { "Refresh" })
    String settings_accountmanager_refresh();

    @Default(lngs = { "en" }, values = { "Renew / Buy new Account" })
    String settings_accountmanager_renew();

    @Default(lngs = { "en" }, values = { "Others" })
    String settings_linkgrabber_filter_others();

    @Default(lngs = { "en" }, values = { "Add" })
    String settings_linkgrabber_filter_action_add();

    @Default(lngs = { "en" }, values = { "Enable/Disable" })
    String settings_linkgrabber_filter_columns_enabled();

    @Default(lngs = { "en" }, values = { "If the file already exists:" })
    String system_download_triggerfileexists();

    @Default(lngs = { "en" }, values = { "Ask for each file" })
    String system_download_triggerfileexists_ask();

    @Default(lngs = { "en" }, values = { "Overwrite" })
    String system_download_triggerfileexists_overwrite();

    @Default(lngs = { "en" }, values = { "Auto rename" })
    String system_download_triggerfileexists_rename();

    @Default(lngs = { "en" }, values = { "Skip Link" })
    String system_download_triggerfileexists_skip();

    @Default(lngs = { "en" }, values = { ".*(error|failed).*" })
    String userio_errorregex();

    @Default(lngs = { "en" }, values = { "No Proxy" })
    String gui_column_proxytype_no_proxy();

    @Default(lngs = { "en" }, values = { "HTTP Proxy" })
    String gui_column_proxytype_http();

    @Default(lngs = { "en" }, values = { "Socks 5 Proxy" })
    String gui_column_proxytype_socks5();

    @Default(lngs = { "en" }, values = { "Use for Proxy Rotation" })
    String gui_column_proxytype_rotation_check();

    @Default(lngs = { "en" }, values = { "Set Defaultproxy here" })
    String gui_column_proxytype_default();

    @Default(lngs = { "en" }, values = { "Direct Gateway" })
    String gui_column_proxytype_direct();

    @Default(lngs = { "en" }, values = { "If you have several external gateways, you can use each like a different external ip." })
    String gui_column_proxytype_direct_tt();

    @Default(lngs = { "en" }, values = { "No Proxy! Use the default direct connection." })
    String gui_column_proxytype_no_proxy_tt();

    @Default(lngs = { "en" }, values = { "Hypertext Transfer Protocol (HTTP Proxy)" })
    String gui_column_proxytype_http_tt();

    @Default(lngs = { "en" }, values = { "SOCKS-5-Protocol Proxy Server " })
    String gui_column_proxytype_socks5_tt();

    @Default(lngs = { "en" }, values = { "SOCKS-4-Protocol Proxy Server " })
    String gui_column_proxytype_socks4_tt();

    @Default(lngs = { "en" }, values = { "Proxy Rotation requires at least one active entry." })
    String proxytablemodel_atleast_one_rotate_required();

    @Default(lngs = { "en" }, values = { "Buy a Premium Account" })
    String buyaction_title();

    @Default(lngs = { "en" }, values = { "Most Hosters offer a \"Premium Mode\". Download in Premium Mode is usually much faster." })
    String buyaction_message();

    @Default(lngs = { "en" }, values = { "Continue" })
    String buyaction_title_buy_account();

    @Default(lngs = { "en" }, values = { "Check Account" })
    String accountdialog_check();

    @Default(lngs = { "en" }, values = { "JDownloader checks if logins are correct" })
    String accountdialog_check_msg();

    @Default(lngs = { "en" }, values = { "Cannot add Account because %s1" })
    String accountdialog_check_invalid(String status);

    @Default(lngs = { "en" }, values = { "Verified logins:\r\n%s1" })
    String accountdialog_check_valid(String status);

    @Default(lngs = { "en" }, values = { "Accountcheck Failed" })
    String accountdialog_check_failed();

    @Default(lngs = { "en" }, values = { "Account check failed.\r\nPlease make sure that your entered logins are correct." })
    String accountdialog_check_failed_msg();

    @Default(lngs = { "en" }, values = { "Accountlogins are correct, but your Account has expired." })
    String accountdialog_check_expired(String status);

    @Default(lngs = { "en" }, values = { "Renew Account?" })
    String accountdialog_check_expired_title();

    @Default(lngs = { "en" }, values = { "Renew/Extend Account now!" })
    String accountdialog_check_expired_renew();

    @Default(lngs = { "en" }, values = { "Enter Username..." })
    String jd_gui_swing_components_AccountDialog_help_username();

    @Default(lngs = { "en" }, values = { "Really remove %s1 account(s)?" })
    String account_remove_action_title(int num);

    @Default(lngs = { "en" }, values = { "Really remove %s1" })
    String account_remove_action_msg(String string);

    @Default(lngs = { "en" }, values = { "n.a." })
    String added_date_column_invalid();

    @Default(lngs = { "en" }, values = { "Added Date" })
    String added_date_column_title();

    @Default(lngs = { "en" }, values = { "Name" })
    String filecolumn_title();

    @Default(lngs = { "en" }, values = { "Finished Date" })
    String FinishedDateColumn_FinishedDateColumn();

    @Default(lngs = { "en" }, values = { "Size" })
    String SizeColumn_SizeColumn();

    @Default(lngs = { "en" }, values = { "Watch As You Download" })
    String WatchAsYouDownloadColumn_WatchAsYouDownloadColumn();

    @Default(lngs = { "en" }, values = { "Bytes Left" })
    String RemainingColumn_RemainingColumn();

    @Default(lngs = { "en" }, values = { "Priority" })
    String PriorityColumn_PriorityColumn();

    @Default(lngs = { "en" }, values = { "Bytes Loaded" })
    String LoadedColumn_LoadedColumn();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String HosterColumn_HosterColumn();

    @Default(lngs = { "en" }, values = { "Status" })
    String StatusColumn_StatusColumn();

    @Default(lngs = { "en" }, values = { "Download Control" })
    String StopSignColumn_StopSignColumn();

    @Default(lngs = { "en" }, values = { "Availability" })
    String AvailabilityColumn_AvailabilityColumn();

    @Default(lngs = { "en" }, values = { "Progress" })
    String ProgressColumn_ProgressColumn();

    @Default(lngs = { "en" }, values = { "ETA" })
    String ETAColumn_ETAColumn();

    @Default(lngs = { "en" }, values = { "Speed" })
    String SpeedColumn_SpeedColumn();

    @Default(lngs = { "en" }, values = { "Move selected Links & Packages to top" })
    String BottomBar_BottomBar_totop_tooltip();

    @Default(lngs = { "en" }, values = { "Move selected Links & Packages up" })
    String BottomBar_BottomBar_moveup_tooltip();

    @Default(lngs = { "en" }, values = { "Move selected Links & Packages down" })
    String BottomBar_BottomBar_movedown_tooltip();

    @Default(lngs = { "en" }, values = { "Move selected Links & Packages to bottom" })
    String BottomBar_BottomBar_tobottom_tooltip();

    @Default(lngs = { "en" }, values = { "Connection" })
    String ConnectionColumn_ConnectionColumn();

    @Default(lngs = { "en" }, values = { "Connection: %s1" })
    String ConnectionColumn_getStringValue_connection(HTTPProxy currentProxy);

    @Default(lngs = { "en" }, values = { "Downloading with %s1 chunk(s)" })
    String ConnectionColumn_getStringValue_chunks(int currentChunks);

    @Default(lngs = { "en" }, values = { "Sorted by '%s1'-Column" })
    String DownloadsTable_actionPerformed_sortwarner_title(String column);

    @Default(lngs = { "en" }, values = { "Your Download list is not in download order any more. \r\nClick twice on the highlighted column header,\r\nto return to default (Top-Down) order." })
    String DownloadsTable_actionPerformed_sortwarner_text();

    @Default(lngs = { "en" }, values = { "Comment" })
    String CommentColumn_CommentColumn_();

    @Default(lngs = { "en" }, values = { "Views" })
    String LinkGrabberSideBarHeader_LinkGrabberSideBarHeader();

    @Default(lngs = { "en" }, values = { "Add New Links" })
    @DescriptionForTranslationEntry("Add Links Button in Linkgrabber bottom left")
    String AddLinksToLinkgrabberAction();

    @Default(lngs = { "en" }, values = { "Restore %s1 filtered Links" })
    String RestoreFilteredLinksAction_(int x);

    @Default(lngs = { "en" }, values = { "Clears the list" })
    String ClearAction_tt_();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String LinkGrabberSidebar_LinkGrabberSidebar_hosterfilter();

    @Default(lngs = { "en" }, values = { "File Types" })
    String LinkGrabberSidebar_LinkGrabberSidebar_extensionfilter();

    @Default(lngs = { "en" }, values = { "Quick Settings" })
    String LinkGrabberSidebar_LinkGrabberSidebar_settings();

    @Default(lngs = { "en" }, values = { "Add at top" })
    String LinkGrabberSidebar_LinkGrabberSidebar_addtop();

    @Default(lngs = { "en" }, values = { "Adds links at top of download list. They will be next in downloadorder" })
    String LinkGrabberSidebar_LinkGrabberSidebar_addtop_tt();

    @Default(lngs = { "en" }, values = { "Auto confirm" })
    String LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm();

    @Default(lngs = { "en" }, values = { "If enabled, Links will be moved to downloadlist automatically after a given timeout" })
    String LinkGrabberSidebar_LinkGrabberSidebar_autoconfirm_tt();

    @Default(lngs = { "en" }, values = { "Autostart Download" })
    String LinkGrabberSidebar_LinkGrabberSidebar_autostart();

    @Default(lngs = { "en" }, values = { "Starts downloading after adding links to the downloadlist" })
    String LinkGrabberSidebar_LinkGrabberSidebar_autostart_tt();

    @Default(lngs = { "en" }, values = { "Load Linkcontainer" })
    String AddContainerAction();

    @Default(lngs = { "en" }, values = { "Analyse Text with Links" })
    String AddOptionsAction_actionPerformed_addlinks();

    @Default(lngs = { "en" }, values = { "Are you sure?" })
    String literally_are_you_sure();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all links from Linkgrabber?" })
    String ClearAction_actionPerformed_msg();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all offline links from Linkgrabber?" })
    String ClearAction_actionPerformed_offline_msg();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all selected links from Linkgrabber?" })
    String ClearAction_actionPerformed_selected_msg();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all not-selected links from Linkgrabber?" })
    String ClearAction_actionPerformed_notselected_msg();

    @Default(lngs = { "en" }, values = { "Yes" })
    String literally_yes();

    @Default(lngs = { "en" }, values = { "No" })
    String literall_no();

    @Default(lngs = { "en" }, values = { "Load a Link Container" })
    String AddContainerAction_actionPerformed_();

    @Default(lngs = { "en" }, values = { "Link Container (%s1)" })
    String AddContainerAction_actionPerformed_extensions(String containerExtensions);

    @Default(lngs = { "en" }, values = { "Download All" })
    String ConfirmOptionsAction_actionPerformed_all();

    @Default(lngs = { "en" }, values = { "Download Selected" })
    String ConfirmOptionsAction_actionPerformed_selected();

    @Default(lngs = { "en" }, values = { "Analyse and Add Links" })
    String AddLinksDialog_AddLinksDialog_();

    @Default(lngs = { "en" }, values = { "Continue" })
    String AddLinksDialog_AddLinksDialog_confirm();

    @Default(lngs = { "en" }, values = { "JDownloader helps you to parse text or websites for links. Enter Links, URLs, Websites, or text below, \r\nchoose a Download Destination, and click 'Start Analyse'. \r\nAll 'downloadable' Files will be listed in the the Linkgrabber view afterwards." })
    String AddLinksDialog_layoutDialogContent_description();

    @Default(lngs = { "en" }, values = { "Enter Links, URLs, Websites, or any other text here..." })
    String AddLinksDialog_layoutDialogContent_input_help();

    @Default(lngs = { "en" }, values = { "Please choose Download Destination here." })
    String AddLinksDialog_layoutDialogContent_save_tt();

    @Default(lngs = { "en" }, values = { "Choose a Package Name for the Downloads above. If empty, JDownloader will create Packages based on the filenames" })
    String AddLinksDialog_layoutDialogContent_package_tt();

    @Default(lngs = { "en" }, values = { "Enter a Package name, or leave empty for auto mode" })
    String AddLinksDialog_layoutDialogContent_packagename_help();

    @Default(lngs = { "en" }, values = { "Enter the archive's extraction password" })
    String AddLinksDialog_createExtracOptionsPanel_password();

    @Default(lngs = { "en" }, values = { "Enable this option to extract all found archives after download" })
    String AddLinksDialog_layoutDialogContent_autoextract_tooltip();

    @Default(lngs = { "en" }, values = { "Start Deep Link Analyse" })
    String ConfirmOptionsAction_actionPerformed_deep();

    @Default(lngs = { "en" }, values = { "Start Normal Link Analyse" })
    String ConfirmOptionsAction_actionPerformed_normale();

    @Default(lngs = { "en" }, values = { "Please enter Urls, Links, Websites, or plain text" })
    String AddLinksDialog_validateForm_input_missing();

    @Default(lngs = { "en" }, values = { "Please make sure to enter a valid Download Destination." })
    String AddLinksDialog_validateForm_folder_invalid_missing();

    @Default(lngs = { "en" }, values = { "Please enter a valid Download Folder" })
    String AddLinksDialog_layoutDialogContent_help_destination();

    @Default(lngs = { "en" }, values = { "Reconnect Activity" })
    String StatusBarImpl_initGUI_reconnect();

    @Default(lngs = { "en" }, values = { "Linkgrabber Activity" })
    String StatusBarImpl_initGUI_linkgrabber();

    @Default(lngs = { "en" }, values = { "Abort Linkgrabber" })
    String StatusBarImpl_initGUI_abort_linkgrabber();

    @Default(lngs = { "en" }, values = { "Extraction Activity" })
    String StatusBarImpl_initGUI_extract();

    @Default(lngs = { "en" }, values = { "JDownloader is crawling links for you. Open Linkgrabber to see the results." })
    String StatusBarImpl_initGUI_linkgrabber_desc();

    @Default(lngs = { "en" }, values = { " - idle - " })
    String StatusBarImpl_initGUI_linkgrabber_desc_inactive();

    @Default(lngs = { "en" }, values = { "Continue" })
    String literally_continue();

    @Default(lngs = { "en" }, values = { "Warning" })
    String literally_warning();

    @Default(lngs = { "en" }, values = { "Error" })
    String literall_error();

    @Default(lngs = { "en" }, values = { "Edit" })
    String literally_edit();

    @Default(lngs = { "en" }, values = { "Test" })
    String ReconnectTestAction();

    @Default(lngs = { "en" }, values = { "Reconnect" })
    String ReconnectDialog_ReconnectDialog_();

    @Default(lngs = { "en" }, values = { "Duration: " })
    String ReconnectDialog_layoutDialogContent_duration();

    @Default(lngs = { "en" }, values = { "Old IP: " })
    String ReconnectDialog_layoutDialogContent_old();

    @Default(lngs = { "en" }, values = { "Reconnect Plugin: %s1" })
    String ReconnectDialog_layoutDialogContent_header(String name);

    @Default(lngs = { "en" }, values = { "Forbidden IP: %s1" })
    String ReconnectDialog_onIPForbidden_(String externalIp);

    @Default(lngs = { "en" }, values = { "Router is Offline" })
    String ReconnectDialog_onIPOffline_();

    @Default(lngs = { "en" }, values = { "Router is Online" })
    String ReconnectDialog_onIPOnline_();

    @Default(lngs = { "en" }, values = { "Reconnect Successful" })
    String ReconnectDialog_onIPValidated_();

    @Default(lngs = { "en" }, values = { "Close" })
    String literally_close();

    @Default(lngs = { "en" }, values = { "-offline-" })
    String literally_offline();

    @Default(lngs = { "en" }, values = { "Current External IP" })
    String ReconnectDialog_layoutDialogContent_currentip();

    @Default(lngs = { "en" }, values = { "Auto Setup Reconnect" })
    String AutoDetectAction_actionPerformed_d_title();

    @Default(lngs = { "en" }, values = { "Automatic Proxy Detection" })
    String ProxyAutoAction_actionPerformed_d_title();

    @Default(lngs = { "en" }, values = { "Please wait. JDownloader tries to find the correct settings for your internet connection. \r\nThis may take several minutes." })
    String AutoDetectAction_actionPerformed_d_msg();

    @Default(lngs = { "en" }, values = { "It seems that you are using a Direct Modem Connection to access the internet.\r\nReconnect for Modem Connections have to be set up manually.\r\n\r\nDo you use a Router or a Modem to connect to the internet?" })
    String AutoSetupAction_actionPerformed_modem();

    @Default(lngs = { "en" }, values = { "Modem/Dial-Up" })
    String AutoSetupAction_actionPerformed_choose_modem();

    @Default(lngs = { "en" }, values = { "I'm not sure!" })
    String AutoSetupAction_actionPerformed_dont_know();

    @Default(lngs = { "en" }, values = { "Router" })
    String AutoSetupAction_actionPerformed_router();

    @Default(lngs = { "en" }, values = { "Auto Setup only works for Router connections. \r\nPlease see the opened Site for Modem Reconnects." })
    String AutoSetupAction_actionPerformed_noautoformodem();

    @Default(lngs = { "en" }, values = { "Reconnect failed. Please check your settings." })
    String ReconnectDialog_layoutDialogContent_error();

    @Default(lngs = { "en" }, values = { "Use found Script" })
    String ReconnectFindDialog_ReconnectFindDialog_ok();

    @Default(lngs = { "en" }, values = { "No working Script found so far." })
    String ReconnectFindDialog_packed_no_found_script_tooltip();

    @Default(lngs = { "en" }, values = { "Stop Scanning, and use the best script found until now." })
    String ReconnectFindDialog_packed_interrupt_tooltip();

    @Default(lngs = { "en" }, values = { "Optimize Reconnect - Do tests to improve timing ..." })
    String AutoDetectAction_actionPerformed_dooptimization_title();

    @Default(lngs = { "en" }, values = { "JDownloader found %s1 working Reconnect Script(s). \r\nTo improve Reconnect Timings, you should do some automatic tests runs. These tests would take about %s2. \r\nIf you skip this step, your current Reconnect Settings will need about %s3 to perform a successful Reconnect." })
    String AutoDetectAction_actionPerformed_dooptimization_msg(int scriptNum, String optiDuration, String bestDuration);

    @Default(lngs = { "en" }, values = { "Continue" })
    String AutoDetectAction_run_optimization();

    @Default(lngs = { "en" }, values = { "Skip" })
    String AutoDetectAction_skip_optimization();

    @Default(lngs = { "en" }, values = { "Found successful Script" })
    String LiveHeaderDetectionWizard_testList_firstSuccess_title();

    @Default(lngs = { "en" }, values = { "JDownloader found a working Reconnect Script. \r\nGetting a new IP took %s1 with this Script. \r\nThere might be a faster one. Do you want JDownloader to continue scanning for faster scripts?" })
    String LiveHeaderDetectionWizard_testList_firstsuccess_msg(String time);

    @Default(lngs = { "en" }, values = { "Yes, please try it!" })
    String LiveHeaderDetectionWizard_testList_ok();

    @Default(lngs = { "en" }, values = { "No, stop search and use this script!" })
    String LiveHeaderDetectionWizard_testList_use();

    @Default(lngs = { "en" }, values = { "Optimizing  %s1" })
    String AutoDetectAction_run_optimize(String name);

    @Default(lngs = { "en" }, values = { "Offline - Waiting for a internet connection!" })
    String LiveaheaderDetection_wait_for_online();

    @Default(lngs = { "en" }, values = { "This Wizard will scan your Network configuration, and try to find Reconnect Scripts from our online Database.\r\nThis may take between a few seconds, and several minutes.\r\nOnly Dynamic IP DSL Connections via a Router can be autodetected." })
    String AutoSetupAction_tt();

    @Default(lngs = { "en" }, values = { "Click here do a Reconnect and get a new External IP." })
    String ReconnectTestAction_tt_2();

    @Default(lngs = { "en" }, values = { "Validating your Reconnect Script..." })
    String LiveHeaderDetectionWizard_sendRouter_havetovalidate();

    @Default(lngs = { "en" }, values = { "Rule Name" })
    String settings_linkgrabber_filter_columns_name();

    @Default(lngs = { "en" }, values = { "Save" })
    String literally_save();

    @Default(lngs = { "en" }, values = { "Rule: %s1" })
    String FilterRuleDialog_FilterRuleDialog_(String name);

    @Default(lngs = { "en" }, values = { "Enter short name for this rule..." })
    String FilterRuleDialog_layoutDialogContent_ht_name();

    @Default(lngs = { "en" }, values = { "equals" })
    String FilterRuleDialog_layoutDialogContent_equals();

    @Default(lngs = { "en" }, values = { "contains" })
    String FilterRuleDialog_layoutDialogContent_contains();

    @Default(lngs = { "en" }, values = { "Enter file name matcher... (Use * as Wildcard)" })
    String FilterRuleDialog_layoutDialogContent_ht_filename();

    @Default(lngs = { "en" }, values = { "File Name" })
    String FilterRuleDialog_layoutDialogContent_lbl_filename();

    @Default(lngs = { "en" }, values = { "File Size" })
    String FilterRuleDialog_layoutDialogContent_lbl_size();

    @Default(lngs = { "en" }, values = { "Unsupported Network Settings?" })
    String ReconnectPluginController_autoFind_gatewaywarning_t();

    @Default(lngs = { "en" }, values = { "It seems that your network setup is not supported by Reconnect Wizard.\r\nThis Wizard is able to find Reconnect Settings for 'DSL via Router' - Connections.\r\nClick [Continue] to try it anyway." })
    String ReconnectPluginController_autoFind_gatewaywarning();

    @Default(lngs = { "en" }, values = { "Checking Network Settings..." })
    String LiveaheaderDetection_network_setup_check();

    @Default(lngs = { "en" }, values = { "Find Router IP..." })
    String LiveaheaderDetection_find_router();

    @Default(lngs = { "en" }, values = { "File Type" })
    String FilterRuleDialog_layoutDialogContent_lbl_type();

    @Default(lngs = { "en" }, values = { "Audio File" })
    String FilterRuleDialog_createTypeFilter_mime_audio();

    @Default(lngs = { "en" }, values = { "Video File" })
    String FilterRuleDialog_createTypeFilter_mime_video();

    @Default(lngs = { "en" }, values = { "Archive" })
    String FilterRuleDialog_createTypeFilter_mime_archives();

    @Default(lngs = { "en" }, values = { "Image" })
    String FilterRuleDialog_createTypeFilter_mime_images();

    @Default(lngs = { "en" }, values = { "Enter comma seperated extensions like 'pdf,html,png,' ... (Use * as Wildcard)" })
    String FilterRuleDialog_createTypeFilter_mime_custom_help();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String FilterRuleDialog_layoutDialogContent_lbl_hoster();

    @Default(lngs = { "en" }, values = { "Enter a URL Expression like 'rapidshare.com'... (Use * as Wildcard)" })
    String FilterRuleDialog_layoutDialogContent_lbl_hoster_help();

    @Default(lngs = { "en" }, values = { "Source" })
    String FilterRuleDialog_layoutDialogContent_lbl_source();

    @Default(lngs = { "en" }, values = { "Enter a Crawler Source Expression like 'jamendo.com'... (Use * as Wildcard)" })
    String FilterRuleDialog_layoutDialogContent_lbl_source_help();

    @Default(lngs = { "en" }, values = { "If the following conditions match ... " })
    String FilterRuleDialog_layoutDialogContent_if();

    @Default(lngs = { "en" }, values = { "Condition Name" })
    String FilterRuleDialog_layoutDialogContent_name();

    @Default(lngs = { "en" }, values = { "%s1!" })
    String settings_linkgrabber_filter_columns_if(String trim);

    @Default(lngs = { "en" }, values = { "contains %s1" })
    String RegexFilter_toString_contains(String regex);

    @Default(lngs = { "en" }, values = { "is %s1" })
    String RegexFilter_toString_matches(String regex);

    @Default(lngs = { "en" }, values = { "doesn't contain %s1" })
    String RegexFilter_toString_contains_not(String regex);

    @Default(lngs = { "en" }, values = { "isn't %s1" })
    String RegexFilter_toString_matches_not(String regex);

    @Default(lngs = { "en" }, values = { "is %s1" })
    String FilesizeFilter_toString_same(String size);

    @Default(lngs = { "en" }, values = { "is between %s1 and %s2" })
    String FilesizeFilter_toString_(String formatBytes, String formatBytes2);

    @Default(lngs = { "en" }, values = { "isn't %s1" })
    String FilesizeFilter_toString_same_not(String size);

    @Default(lngs = { "en" }, values = { "isn't between %s1 and %s2" })
    String FilesizeFilter_toString_not(String formatBytes, String formatBytes2);

    @Default(lngs = { "en" }, values = { "Filename %s1" })
    String FilterRule_toString_name(String string);

    @Default(lngs = { "en" }, values = { "Packagename %s1" })
    String FilterRule_toString_packagename(String string);

    @Default(lngs = { "en" }, values = { "\r\n%s1 %s2" })
    String FilterRule_toString_name2(String name, String string);

    @Default(lngs = { "en" }, values = { "\r\n%s1 %s2" })
    String FilterRule_toString_package2(String name, String string);

    @Default(lngs = { "en" }, values = { "Hoster URL %s1" })
    String FilterRule_toString_hoster(String string);

    @Default(lngs = { "en" }, values = { "\r\nURL(%s1) %s2" })
    String FilterRule_toString_hoster2(String a, String string);

    @Default(lngs = { "en" }, values = { "Source URL %s1" })
    String FilterRule_toString_source(String string);

    @Default(lngs = { "en" }, values = { ",\r\n%s1" })
    String FilterRule_toString_comma2(String string);

    @Default(lngs = { "en" }, values = { " and \r\n%s1" })
    String FilterRule_toString_and2(String string);

    @Default(lngs = { "en" }, values = { "Filter Links if, " })
    String settings_linkgrabber_filter_columns_condition();

    @Default(lngs = { "en" }, values = { "Size %s1" })
    String FilterRule_toString_size(String string);

    @Default(lngs = { "en" }, values = { "\r\nSize(%s1) %s2" })
    String FilterRule_toString_size2(String string, String string2);

    @Default(lngs = { "en" }, values = { "File %s1" })
    String FilterRule_toString_type(String string);

    @Default(lngs = { "en" }, values = { "\r\n%s1-File %s2" })
    String FilterRule_toString_type2(String ext, String string);

    @Default(lngs = { "en" }, values = { "'%s1'-File" })
    String FiletypeFilter_toString_custom(String customs);

    @Default(lngs = { "en" }, values = { " or %s1" })
    String FilterRule_toString_or(String string);

    @Default(lngs = { "en" }, values = { "is a %s1" })
    String FiletypeFilter_toString_(String string);

    @Default(lngs = { "en" }, values = { "isn't a %s1" })
    String FiletypeFilter_toString_not(String string);

    @Default(lngs = { "en" }, values = { "Packagizer" })
    String gui_config_linkgrabber_packagizer();

    @Default(lngs = { "en" }, values = { "Add" })
    String literally_add();

    @Default(lngs = { "en" }, values = { "Remove" })
    String literally_remove();

    @Default(lngs = { "en" }, values = { "Download Directory" })
    String PackagizerFilterRuleDialog_layoutDialogContent_dest();

    @Default(lngs = { "en" }, values = { "Move to" })
    String PackagizerFilterRuleDialog_layoutDialogContent_move();

    @Default(lngs = { "en" }, values = { "Priority" })
    String PackagizerFilterRuleDialog_layoutDialogContent_priority();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String PackagizerFilterRuleDialog_layoutDialogContent_packagename();

    @Default(lngs = { "en" }, values = { "Enable Download" })
    String PackagizerFilterRuleDialog_layoutDialogContent_enable();

    @Default(lngs = { "en" }, values = { "Extract Archives" })
    String PackagizerFilterRuleDialog_layoutDialogContent_extract();

    @Default(lngs = { "en" }, values = { "Chunks/Connections" })
    String PackagizerFilterRuleDialog_layoutDialogContent_chunks();

    @Default(lngs = { "en" }, values = { "... then set (before downloading)" })
    String PackagizerFilterRuleDialog_layoutDialogContent_then();

    @Default(lngs = { "en" }, values = { "... then do ( these actions only apply to extracted files" })
    String PackagizerFilterRuleDialog_layoutDialogContent_do2();

    @Default(lngs = { "en" }, values = { "Date or Time" })
    String PackagizerFilterRuleDialog_createVariablesMenu_date();

    @Default(lngs = { "en" }, values = { "Dynamic Variables" })
    String PackagizerFilterRuleDialog_createVariablesMenu_menu();

    @Default(lngs = { "en" }, values = { "Original Filename Wildcard(*) #%s1" })
    String PackagizerFilterRuleDialog_createVariablesMenu_filename(int i);

    @Default(lngs = { "en" }, values = { "Hoster Url Wildcard(*) #%s1" })
    String PackagizerFilterRuleDialog_createVariablesMenu_hoster(int i);

    @Default(lngs = { "en" }, values = { "Source Url Wildcard(*) #%s1" })
    String PackagizerFilterRuleDialog_createVariablesMenu_source(int i);

    @Default(lngs = { "en" }, values = { "Packagename" })
    String PackagizerFilterRuleDialog_createVariablesMenu_packagename();

    @Default(lngs = { "en" }, values = { "Auto Start Download" })
    String PackagizerFilterRuleDialog_layoutDialogContent_autostart2();

    @Default(lngs = { "en" }, values = { "Auto Confirm" })
    String PackagizerFilterRuleDialog_layoutDialogContent_autoadd2();

    @Default(lngs = { "en" }, values = { "Settings" })
    String SettingsMenu_SettingsMenu_();

    @Default(lngs = { "en" }, values = { "Max. Chunks per Download" })
    String ChunksEditor_ChunksEditor_();

    @Default(lngs = { "en" }, values = { "Max. simultaneous Downloads" })
    String ParalellDownloadsEditor_ParalellDownloadsEditor_();

    @Default(lngs = { "en" }, values = { "Speed Limit" })
    String SpeedlimitEditor_SpeedlimitEditor_();

    @Default(lngs = { "en" }, values = { "%s1/s" })
    String SpeedlimitEditor_format(String formatBytes);

    @Default(lngs = { "en" }, values = { "Open Quicksettings" })
    String BottomBar_BottomBar_settings();

    @Default(lngs = { "en" }, values = { "Max. simultaneous Downloads" })
    String gui_config_download_simultan_downloads();

    @Default(lngs = { "en" }, values = { "Max. Chunks per Download" })
    String gui_config_download_max_chunks();

    @Default(lngs = { "en" }, values = { "Speedlimit: %s1/s" })
    String SpeedMeterPanel_createTooltipText_(String formatBytes);

    @Default(lngs = { "en" }, values = { "Import" })
    String LinkgrabberFilter_LinkgrabberFilter_import();

    @Default(lngs = { "en" }, values = { "Import Filterrules" })
    String LinkgrabberFilter_import_dialog_title();

    @Default(lngs = { "en" }, values = { "Default Rules" })
    String LinkgrabberFilter_default_rules();

    @Default(lngs = { "en" }, values = { "Rule File %s1 is empty or is invalid." })
    String LinkgrabberFilter_LinkgrabberFilter_import_invalid(String name);

    @Default(lngs = { "en" }, values = { "Export" })
    String LinkgrabberFilter_LinkgrabberFilter_export();

    @Default(lngs = { "en" }, values = { "Export to File" })
    String LinkgrabberFilter_export_dialog_title();

    @Default(lngs = { "en" }, values = { "Are you sure?" })
    String literall_are_you_sure();

    @Default(lngs = { "en" }, values = { "Global Link Filter" })
    String LinkGrabberSidebar_LinkGrabberSidebar_globfilter();

    @Default(lngs = { "en" }, values = { "Enable/Disable global Linkgrabber Filter (see Settings)" })
    String LinkGrabberSidebar_LinkGrabberSidebar_globfilter_tt();

    @Default(lngs = { "en" }, values = { "Hate Captchas? Click here!" })
    String CaptchaDialog_getDefaultButtonPanel_premium();

    @Default(lngs = { "en" }, values = { "File '%s1'(%s2) from %s3" })
    String CaptchaDialog_layoutDialogContent_header(String filename, String formatBytes, String hoster);

    @Default(lngs = { "en" }, values = { "File '%s1' from %s3" })
    String CaptchaDialog_layoutDialogContent_header2(String filename, String hoster);

    @Default(lngs = { "en" }, values = { "Avoid %s1 Captchas" })
    String PremiumInfoDialog_PremiumInfoDialog_(String tld);

    @Default(lngs = { "en" }, values = { "JDownloader is able to auto recognize most of the Captchas out there.\r\nHowever, %s1 unfortunately uses a Captcha Type which cannot be recognized.\r\nTo download without interruptions, we recommend to use %s1's Premium Mode." })
    String PremiumInfoDialog_layoutDialogContent_explain(String name);

    @Default(lngs = { "en" }, values = { "Unlimited Downloadspeed - use your full Internet Bandwidth!" })
    String PremiumFeature_speed_();

    @Default(lngs = { "en" }, values = { "Unlimited Traffic - Download as much as you want." })
    String PremiumFeature_bandwidth_();

    @Default(lngs = { "en" }, values = { "Download files in parallel" })
    String PremiumFeature_parallel_();

    @Default(lngs = { "en" }, values = { "Resume stopped or broken Downloads" })
    String PremiumFeature_resume_();

    @Default(lngs = { "en" }, values = { "Boost Downloadspeed with Chunkload - Use several connections per file" })
    String PremiumFeature_chunkload_();

    @Default(lngs = { "en" }, values = { "Besides avoiding annoying Captchas, there are further benefits of using Premium:" })
    String PremiumInfoDialog_layoutDialogContent_advantages_header();

    @Default(lngs = { "en" }, values = { "No Waittime before or between Downloads" })
    String PremiumFeature_noWaittime_();

    @Default(lngs = { "en" }, values = { "Give it a try" })
    String PremiumInfoDialog_layoutDialogContent_interested();

    @Default(lngs = { "en" }, values = { "No Thanks" })
    String literall_no_thanks();

    @Default(lngs = { "en" }, values = { "1. Get a Premium Account" })
    String BuyAndAddPremiumAccount_layoutDialogContent_get();

    @Default(lngs = { "en" }, values = { "2. Enter your Logins" })
    String BuyAndAddPremiumAccount_layoutDialogContent_enter();

    @Default(lngs = { "en" }, values = { "Click here to get an Premium Account" })
    String OpenURLAction_OpenURLAction_();

    @Default(lngs = { "en" }, values = { "Enter password..." })
    String BuyAndAddPremiumAccount_layoutDialogContent_pass();

    @Default(lngs = { "en" }, values = { "Max. sim. Downloads per Hoster" })
    String ParalellDownloadsEditor_ParallelDownloadsPerHostEditor_();

    @Default(lngs = { "en" }, values = { "Enable/Disable this option" })
    String AbstractConfigPanel_addPair_enabled();

    @Default(lngs = { "en" }, values = { "Enter Filename Filter Expression..." })
    String SearchField_SearchField_helptext();

    @Default(lngs = { "en" }, values = { "Start Downloads" })
    String ConfirmAction_ConfirmAction_context_add_and_start();

    @Default(lngs = { "en" }, values = { "Add to Download List" })
    String ConfirmAction_ConfirmAction_context_add();

    @Default(lngs = { "en" }, values = { "Open in Browser" })
    String OpenUrlAction_OpenUrlAction_();

    @Default(lngs = { "en" }, values = { "Save to" })
    String LinkGrabberTableModel_initColumns_folder();

    @Default(lngs = { "en" }, values = { "Download from" })
    String LinkGrabberTableModel_initColumns_url();

    @Default(lngs = { "en" }, values = { "Click to open Url in browser" })
    String UrlColumn_UrlColumn_open_tt_();

    @Default(lngs = { "en" }, values = { "Move to new Package" })
    String MergeToPackageAction_MergeToPackageAction_();

    @Default(lngs = { "en" }, values = { "Set Download Directory" })
    String SetDownloadFolderAction_SetDownloadFolderAction_();

    @Default(lngs = { "en" }, values = { "Watch As You Download" })
    String WatchAsYouDownload_WatchAsYouDownloadAction_();

    @Default(lngs = { "en" }, values = { "Enable" })
    String EnabledAction_EnabledAction_enable();

    @Default(lngs = { "en" }, values = { "Disable" })
    String EnabledAction_EnabledAction_disable();

    @Default(lngs = { "en" }, values = { "Set Download Password" })
    String SetDownloadPassword_SetDownloadPassword_();

    @Default(lngs = { "en" }, values = { "Download Password" })
    String DownloadPasswordColumn_DownloadPasswordColumn_object_();

    @Default(lngs = { "en" }, values = { "Delete selected" })
    String ContextMenuFactory_createPopup_cleanup_only();

    @Default(lngs = { "en" }, values = { "Other" })
    String ContextMenuFactory_createPopup_other();

    @Default(lngs = { "en" }, values = { "Offline Links" })
    String RemoveOfflineAction_RemoveOfflineAction_object_();

    @Default(lngs = { "en" }, values = { "Delete all visible Links (Keep filtered)" })
    String RemoveAllAction_RemoveAllAction_object();

    @Default(lngs = { "en" }, values = { "Incomplete Split-Archives" })
    String RemoveIncompleteArchives_RemoveIncompleteArchives_object_();

    @Default(lngs = { "en" }, values = { "Keep only selected Links" })
    String RemoveNonSelectedAction_RemoveNonSelectedAction_object_();

    @Default(lngs = { "en" }, values = { "Split Packages By Hoster" })
    String SplitPackagesByHost_SplitPackagesByHost_object_();

    @Default(lngs = { "en" }, values = { "Sort Package(s) on '%s1'" })
    String SortAction_SortAction_object_(String columnName);

    @Default(lngs = { "en" }, values = { "Crawl %s1-Link" })
    String CaptchaDialog_layoutDialogContent_header_crawler(String tld);

    @Default(lngs = { "en" }, values = { "Crawl %s1 @ %s2" })
    String CaptchaDialog_layoutDialogContent_header_crawler2(String crawlerStatus, String tld);

    @Default(lngs = { "en" }, values = { "Filter List - filter all links that match these rules." })
    String LinkgrabberFilter_initComponents_filter_();

    @Default(lngs = { "en" }, values = { "Views - Grab all links that match these rules anyway." })
    String LinkgrabberFilter_initComponents_exceptions_();

    @Default(lngs = { "en" }, values = { "contains not" })
    String FilterRuleDialog_layoutDialogContent_contains_not();

    @Default(lngs = { "en" }, values = { "equals not" })
    String FilterRuleDialog_layoutDialogContent_equals_not();

    @Default(lngs = { "en" }, values = { "is between" })
    String FilterRuleDialog_layoutDialogContent_is_between();

    @Default(lngs = { "en" }, values = { "is not between" })
    String FilterRuleDialog_layoutDialogContent_is_not_between();

    @Default(lngs = { "en" }, values = { "is" })
    String FilterRuleDialog_layoutDialogContent_is_type();

    @Default(lngs = { "en" }, values = { "is not" })
    String FilterRuleDialog_layoutDialogContent_is_not_type();

    @Default(lngs = { "en" }, values = { "Filter Rule" })
    String FilterRuleDialog_FilterRuleDialog_title_();

    @Default(lngs = { "en" }, values = { "Filter all Links matching these conditions" })
    String FilterRuleDialog_getIfText_();

    @Default(lngs = { "en" }, values = { "Filter Exception Rule" })
    String ExceptionsRuleDialog_ExceptionsRuleDialog_title_();

    @Default(lngs = { "en" }, values = { "Allow all Links matching these conditions, and ignore the Filter List" })
    String ExceptionsRuleDialog_getIfText_();

    @Default(lngs = { "en" }, values = { "Allow Links if, " })
    String ExceptionsTableModel_initColumns_condition_();

    @Default(lngs = { "en" }, values = { "Test filter Rules" })
    String LinkgrabberFilter_LinkgrabberFilter_test_();

    @Default(lngs = { "en" }, values = { "Enter Downloadlink to test Filters..." })
    String LinkgrabberFilter_LinkgrabberFilter_test_help_();

    @Default(lngs = { "en" }, values = { "Test Filters - running" })
    String TestWaitDialog_TestWaitDialog_title_();

    @Default(lngs = { "en" }, values = { "Result:" })
    String TestWaitDialog_layoutDialogContent_filtered();

    @Default(lngs = { "en" }, values = { "%s1/%s2 Links filtered  (%s3 %)" })
    String TestWaitDialog_runInEDT_(int filtered, int size, double d);

    @Default(lngs = { "en" }, values = { "Testlink:" })
    String TestWaitDialog_layoutDialogContent_testlink_();

    @Default(lngs = { "en" }, values = { "Url" })
    String ResultTableModel_initColumns_link_();

    @Default(lngs = { "en" }, values = { "Filtered Link" })
    String ResultTableModel_getTooltipText_dropped_();

    @Default(lngs = { "en" }, values = { "Accepted Link" })
    String ResultTableModel_getTooltipText_accept_();

    @Default(lngs = { "en" }, values = { "Matching Rule Name" })
    String ResultTableModel_initColumns_rule_();

    @Default(lngs = { "en" }, values = { "Test Filters - finished" })
    String TestWaitDialog_TestWaitDialog_title_finished();

    @Default(lngs = { "en" }, values = { "Matching Rule Condition" })
    String ResultTableModel_initColumns_ruledesc_();

    @Default(lngs = { "en" }, values = { "Original Filename" })
    String ResultTableModel_initColumns_filename_();

    @Default(lngs = { "en" }, values = { "Filesize" })
    String ResultTableModel_initColumns_size_();

    @Default(lngs = { "en" }, values = { "Filetype" })
    String ResultTableModel_initColumns_filetype_();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String ResultTableModel_initColumns_hoster();

    @Default(lngs = { "en" }, values = { "Source" })
    String ResultTableModel_initColumns_source();

    @Default(lngs = { "en" }, values = { "Filtered" })
    String ResultTableModel_getStringValue_filtered_();

    @Default(lngs = { "en" }, values = { "Accepted" })
    String ResultTableModel_getStringValue_accepted_();

    @Default(lngs = { "en" }, values = { "No Available Downloads found! Retry with a different Testlink." })
    String TestWaitDialog_runInEDTnothing_found();

    @Default(lngs = { "en" }, values = { "is" })
    String ConditionDialog_layoutDialogContent_online_is_();

    @Default(lngs = { "en" }, values = { "isn't" })
    String ConditionDialog_layoutDialogContent_online_isnot();

    @Default(lngs = { "en" }, values = { "currently uncheckable - Download may be possible." })
    String ConditionDialog_layoutDialogContent_uncheckable_();

    @Default(lngs = { "en" }, values = { "online - Download is possible." })
    String ConditionDialog_layoutDialogContent_online_();

    @Default(lngs = { "en" }, values = { "offline - Download not possible." })
    String ConditionDialog_layoutDialogContent_offline_();

    @Default(lngs = { "en" }, values = { "File" })
    String FilterRuleDialog_layoutDialogContent_lbl_online();

    @Default(lngs = { "en" }, values = { "File is offline" })
    String FilterRule_toString_offline();

    @Default(lngs = { "en" }, values = { "File is online" })
    String FilterRule_toString_online();

    @Default(lngs = { "en" }, values = { "Filestatus cannot be checked" })
    String FilterRule_toString_uncheckable();

    @Default(lngs = { "en" }, values = { "File isn't offline" })
    String FilterRule_toString_offline_not();

    @Default(lngs = { "en" }, values = { "File isn't online" })
    String FilterRule_toString_online_not();

    @Default(lngs = { "en" }, values = { "Filestatus is known" })
    String FilterRule_toString_uncheckable_not();

    @Default(lngs = { "en" }, values = { "Online Status" })
    String ResultTableModel_initColumns_online_();

    @Default(lngs = { "en" }, values = { "Filtered/Accepted" })
    String ResultTableModel_initColumns_filtered_();

    @Default(lngs = { "en" }, values = { "This condition requires that the file is online." })
    String ConditionDialog_updateOnline_linkcheck_required();

    @Default(lngs = { "en" }, values = { "Custom Views" })
    String LinkGrabberSidebar_LinkGrabberSidebar_exceptionfilter();

    @Default(lngs = { "en" }, values = { "Choose Icon for this Filter" })
    String ConditionDialog_layoutDialogContent_object_();

    @Default(lngs = { "en" }, values = { "Show/Hide Sidebar" })
    String LinkGrabberPanel_LinkGrabberPanel_btn_showsidebar_tt_up();

    @Default(lngs = { "en" }, values = { "Sidebar Hidden!" })
    String LinkGrabberPanel_onConfigValueModified_title_();

    @Default(lngs = { "en" }, values = { "The Sidebar is hidden now. Click again to show it.\r\n\r\nWARNING: Views and Settings are still active! Check advanced Settings to disable the sidebar and all it's features completely." })
    String LinkGrabberPanel_onConfigValueModified_msg_();

    @Default(lngs = { "en" }, values = { "Show Selection" })
    String EnabledAllAction_EnabledAllAction_object_show();

    @Default(lngs = { "en" }, values = { "Hide Selection" })
    String EnabledAllAction_EnabledAllAction_object_hide();

    @Default(lngs = { "en" }, values = { "Filename" })
    String PackagizerFilterRuleDialog_layoutDialogContent_filename();

    @Default(lngs = { "en" }, values = { "Rename" })
    String PackagizerFilterRuleDialog_layoutDialogContent_rename();

    @Default(lngs = { "en" }, values = { "Enter Package Name Pattern..." })
    String PackagizerFilterRuleDialog_layoutDialogContent_packagename_help_();

    @Default(lngs = { "en" }, values = { "Enter Filename Pattern..." })
    String PackagizerFilterRuleDialog_layoutDialogContent_filename_help_();

    @Default(lngs = { "en" }, values = { "Enter absolute or relative Path..." })
    String PackagizerFilterRuleDialog_layoutDialogContent_dest_help();

    @Default(lngs = { "en" }, values = { "Original Filename" })
    String PackagizerFilterRuleDialog_createVariablesMenu_filename_org();

    @Default(lngs = { "en" }, values = { "Original Filetype" })
    String PackagizerFilterRuleDialog_createVariablesMenu_filetype_org();

    @Default(lngs = { "en" }, values = { "Priority" })
    String settings_linkgrabber_filter_columns_exepriority();

    @Default(lngs = { "en" }, values = { "Rules with higher Priority always overwrite lower ones." })
    String FilterTableModel_getTooltipText_prio_();

    @Default(lngs = { "en" }, values = { "Condition" })
    String settings_linkgrabber_filter_columns_cond();

    @Default(lngs = { "en" }, values = { "%s1 (Invalid Condition)" })
    String FilterTableModel_getStringValue_name_invalid(String name);

    @Default(lngs = { "en" }, values = { "Invalid Condition - Please check rule!" })
    String FilterTableModel_initColumns_invalid_condition_();

    @Default(lngs = { "en" }, values = { "Enabled" })
    String PackagizerFilterRuleDialog_updateGUI_enabled_();

    @Default(lngs = { "en" }, values = { "Disabled" })
    String PackagizerFilterRuleDialog_updateGUI_disabled_();

    @Default(lngs = { "en" }, values = { "Crawling for Downloads" })
    String AddLinksProgress_AddLinksProgress_();

    @Default(lngs = { "en" }, values = { "Searching for Downloads in %s1..." })
    String AddLinksProgress_layoutDialogContent_header_(String string);

    @Default(lngs = { "en" }, values = { "Duration:" })
    String AddLinksProgress_layoutDialogContent_duration();

    @Default(lngs = { "en" }, values = { "Downloads found:" })
    String AddLinksProgress_found();

    @Default(lngs = { "en" }, values = { "Filtered Links - Check LinkFilter Settings" })
    String AddLinksProgress_filter();

    @Default(lngs = { "en" }, values = { "Hide" })
    String literally_hide();

    @Default(lngs = { "en" }, values = { "Abort" })
    String literally_abort();

    @Default(lngs = { "en" }, values = { "Crawler Window hidden!" })
    String AddLinksProgress_setReturnmask_title_();

    @Default(lngs = { "en" }, values = { "You hid the Crawler Window. \r\nNo reason to worry! You can see the Linkcrawler Status here." })
    String AddLinksProgress_setReturnmask_msg_();

    @Default(lngs = { "en" }, values = { "Permanently Offline" })
    String Permanently_Offline_Package();

    @Default(lngs = { "en" }, values = { "Please wait: Parsing Clipboard content" })
    String AddLinksDialog_ParsingClipboard();

    @Default(lngs = { "en" }, values = { "Enable/Disable Regular Expressions for this Condition" })
    String ConditionDialog_layoutDialogContent_regex_tooltip_();

    @Default(lngs = { "en" }, values = { "Your conditions are not valid. Please check expressions in the highlighted fields." })
    String ConditionDialog_validate_object_();

    @Default(lngs = { "en" }, values = { "Test %s1 Expression" })
    String TestAction_TestAction_object_(String name);

    @Default(lngs = { "en" }, values = { "Please enter %s1" })
    String TestAction_actionPerformed_test_title_(String str);

    @Default(lngs = { "en" }, values = { "To test your expression %s1, please enter the matching %s2" })
    String TestAction_actionPerformed_msg_(String expression, String name);

    @Default(lngs = { "en" }, values = { "Source Url" })
    String ConditionDialog_getPopupMenu_sourceurl_();

    @Default(lngs = { "en" }, values = { "Your Input %s1 does not equal %s2!" })
    String TestAction_actionPerformed_nomatch_(String input, String pattern);

    @Default(lngs = { "en" }, values = { "Your Input %s1 does not contain %s2!" })
    String TestAction_actionPerformed_nomatch_contain(String input, String pattern);

    @Default(lngs = { "en" }, values = { "%s1: %s2" })
    String TestAction_actionPerformed_match_(int i, String m);

    @Default(lngs = { "en" }, values = { "Your input %s1 contains %s2. \r\nWildcard Matches:\r\n%s3" })
    String TestAction_actionPerformed_object_(String input, String pattern, String string);

    @Default(lngs = { "en" }, values = { "Your input %s1 equals %s2. \r\nWildcard Matches:\r\n%s3" })
    String TestAction_actionPerformed_object_matches(String input, String pattern, String string);

    @Default(lngs = { "en" }, values = { "Hoster Url" })
    String ConditionDialog_getPopupMenu_hosterurl_();

    @Default(lngs = { "en" }, values = { "Filename" })
    String ConditionDialog_getPopupMenu_filename_();

    @Default(lngs = { "en" }, values = { "Your input %s1 contains %s2!" })
    String TestAction_actionPerformed_contains_(String input, String pattern);

    @Default(lngs = { "en" }, values = { "Your input %s1 equals %s2!" })
    String TestAction_actionPerformed_equals_(String input, String pattern);

    @Default(lngs = { "en" }, values = { "Filename %s1 matches your Filetype Filter: %s2." })
    String TestAction_actionPerformed_match_ext_(String input, String ext);

    @Default(lngs = { "en" }, values = { "Filename %s1 does not match your Filetype Filter." })
    String TestAction_actionPerformed_nomatch_ext_(String input);

    @Default(lngs = { "en" }, values = { "has" })
    String ConditionDialog_layoutDialogContent_online_has_();

    @Default(lngs = { "en" }, values = { "hasn't" })
    String ConditionDialog_layoutDialogContent_online_hasnot_();

    @Default(lngs = { "en" }, values = { "a valid Premium Account" })
    String ConditionDialog_layoutDialogContent_premium();

    @Default(lngs = { "en" }, values = { "Plugin" })
    String FilterRuleDialog_layoutDialogContent_lbl_plugin();

    @Default(lngs = { "en" }, values = { "a Captcha Solver" })
    String ConditionDialog_layoutDialogContent_captcha();

    @Default(lngs = { "en" }, values = { "Valid Premiumaccount is available" })
    String FilterRule_toString_premium();

    @Default(lngs = { "en" }, values = { "Captchas are solved automatically" })
    String FilterRule_toString_autocaptcha();

    @Default(lngs = { "en" }, values = { "No valid Premiumaccount available" })
    String FilterRule_toString_premium_not();

    @Default(lngs = { "en" }, values = { "Captchas must be entered manually" })
    String FilterRule_toString_autocaptcha_not();

    @Default(lngs = { "en" }, values = { "%s1/%s2 online" })
    String AvailabilityColumn_getStringValue_object_(int i, int size);

    @Default(lngs = { "en" }, values = { "Set Priority" })
    String PriorityAction_PriorityAction_();

    @Default(lngs = { "en" }, values = { "Downloads will start soon. Click here to cancel countdown." })
    String AutoConfirmButton_AutoConfirmButton_tooltip_();

    @Default(lngs = { "en" }, values = { "Reconnect Setup is invalid. Check Settings!" })
    String ReconnectDialog_run_failed_not_setup_();

    @Default(lngs = { "en" }, values = { "Skip file" })
    String IfFileExistsDialog_layoutDialogContent_skip_();

    @Default(lngs = { "en" }, values = { "Overwrite existing file" })
    String IfFileExistsDialog_layoutDialogContent_overwrite_();

    @Default(lngs = { "en" }, values = { "Rename file" })
    String IfFileExistsDialog_layoutDialogContent_rename_();

    @Default(lngs = { "en" }, values = { "Remember selection for this Package" })
    String IfFileExistsDialog_getDontShowAgainLabelText_();

    @Default(lngs = { "en" }, values = { "Enter Test Url..." })
    String PackagizerFilterRuleDialog_PackagizerFilterRuleDialog_test_help();

    @Default(lngs = { "en" }, values = { "Test Filter: \"%s1\"" })
    String FilterRuleDialog_runTest_title_(String string);

    @Default(lngs = { "en" }, values = { "Matches" })
    String ViewTestResultTableModel_initColumns_matches_();

    @Default(lngs = { "en" }, values = { "Packigizer Rule Test: \"%s1\"" })
    String PackagizerRuleDialog_runTest_title_(String string);

    @Default(lngs = { "en" }, values = { "Matches" })
    String PackagizerSingleTestTableModel_initColumns_matches_();

    @Default(lngs = { "en" }, values = { "Download Folder" })
    String PackagizerSingleTestTableModel_initColumns_downloadfolder_();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String PackagizerSingleTestTableModel_initColumns_packagename_();

    @Default(lngs = { "en" }, values = { "File Name" })
    String PackagizerSingleTestTableModel_initColumns_filename_();

    @Default(lngs = { "en" }, values = { "File Name" })
    String searchcategory_filename();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String searchcategory_hoster();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String searchcategory_package();

    @Default(lngs = { "en" }, values = { "Running Downloads" })
    String downloadview_running();

    @Default(lngs = { "en" }, values = { "All Downloads" })
    String downloadview_all();

    @Default(lngs = { "en" }, values = { "Failed Downloads" })
    String downloadview_failed();

    @Default(lngs = { "en" }, values = { "Successful Downloads" })
    String downloadview_successful();

    @Default(lngs = { "en" }, values = { "Please enter the file name you are looking for..." })
    String searchcategory_filename_help();

    @Default(lngs = { "en" }, values = { "Please enter the domain you are looking for..." })
    String searchcategory_hoster_help();

    @Default(lngs = { "en" }, values = { "Please enter the package name you are looking for..." })
    String searchcategory_package_help();

    @Default(lngs = { "en" }, values = { "Choose a View to filter the download list..." })
    String PseudoCombo_PseudoCombo_tt_();

    @Default(lngs = { "en" }, values = { "Renamed Multiarchive!" })
    String FileColumn_setStringValue_title_();

    @Default(lngs = { "en" }, values = { "You renamed one file of an archive that has several parts.\r\nYou have to rename all parts in order to keep the archive extractable!" })
    String FileColumn_setStringValue_msg_();

    @Default(lngs = { "en" }, values = { "Chunks Progress:" })
    String ProgressColumn_createToolTip_object_();

    @Default(lngs = { "en" }, values = { "Always" })
    String gui_config_general_AutoDownloadStartOption_always();

    @Default(lngs = { "en" }, values = { "Only if downloads were running at last Session's end" })
    String gui_config_general_AutoDownloadStartOption_only_if_closed_running();

    @Default(lngs = { "en" }, values = { "Never" })
    String gui_config_general_AutoDownloadStartOption_never();

    @Default(lngs = { "en" }, values = { "Autostart Downloads at Application Start" })
    String system_download_autostart();

    @Default(lngs = { "en" }, values = { "Show Countdown (seconds)" })
    String system_download_autostart_countdown();

    @Default(lngs = { "en" }, values = { "Autostart Downloads" })
    String gui_config_download_autostart();

    @Default(lngs = { "en" }, values = { "Choose if, and when JDownloader should start pending downloads without user interaction..." })
    String gui_config_download_autostart_desc();

    @Default(lngs = { "en" }, values = { "Checking for Updates" })
    String JDUpdater_JDUpdater_object_icon();

    @Default(lngs = { "en" }, values = { "Password required" })
    String JDGui_setVisible_password_();

    @Default(lngs = { "en" }, values = { "Enter the Password to open JDownloader:" })
    String JDGui_setVisible_password_msg();

    @Default(lngs = { "en" }, values = { "The entered Password was wrong!" })
    String JDGui_setVisible_password_wrong();

    @Default(lngs = { "en" }, values = { "New Package" })
    String MergeToPackageAction_actionPerformed_newpackage_();

    @Default(lngs = { "en" }, values = { "Archive Incomplete: %s1" })
    String ConfirmAction_run_incomplete_archive_title_(String name);

    @Default(lngs = { "en" }, values = { "You added an Archive that has several parts. \r\nYou need at least one Link for each part. Some Links are missing." })
    String ConfirmAction_run_incomplete_archive_msg();

    @Default(lngs = { "en" }, values = { "Continue anyway" })
    String ConfirmAction_run_incomplete_archive_continue();

    @Default(lngs = { "en" }, values = { "Show details" })
    String ConfirmAction_run_incomplete_archive_details();

    @Default(lngs = { "en" }, values = { "Stop Mark for Download Links" })
    String StopsignAction_actionPerformed_help_title_();

    @Default(lngs = { "en" }, values = { "If you set a Stop Mark on a Download Link, JDownloader will stop downloading after this file has been loaded.\r\nIf you want to download until this Package is finished, you should set the Stop Mark on the Package instead." })
    String StopsignAction_actionPerformed_help_msg_();

    @Default(lngs = { "en" }, values = { "Stop Mark for Packages" })
    String StopsignAction_actionPerformed_help_title_package_();

    @Default(lngs = { "en" }, values = { "If you set a Stop Mark on a Package, JDownloader will stop downloading after all files in this Package have finished." })
    String StopsignAction_actionPerformed_help_msg_package_();

    @Default(lngs = { "en" }, values = { "Download next" })
    String gui_table_contextmenu_SuperPriorityDownloadAction();

    @Default(lngs = { "en" }, values = { "Save DLC File to..." })
    String CreateDLCAction_actionPerformed_title_();

    @Default(lngs = { "en" }, values = { "Created DLC File: %s1" })
    String DLCFactory_createDLC_created_(String absolutePath);

    @Default(lngs = { "en" }, values = { "Properties" })
    String ContextMenuFactory_createPopup_properties_package();

    @Default(lngs = { "en" }, values = { "Download Speed Limit" })
    String ContextMenuFactory_createPopup_speed();

    @Default(lngs = { "en" }, values = { "Custom Speed Limit" })
    String CustomSpeedLimitator_SpeedlimitEditor__lbl();

    @Default(lngs = { "en" }, values = { "Show Download URLs" })
    String ContextMenuFactory_createPopup_url();

    @Default(lngs = { "en" }, values = { "Source URL for this Download Link" })
    String LinkURLEditor();

    @Default(lngs = { "en" }, values = { "Copy URLs" })
    String LinkURLEditor_onContextMenu_copy_();

    @Default(lngs = { "en" }, values = { "Download Directory for %s1" })
    String OpenDownloadFolderAction_actionPerformed_object_(String string);

    @Default(lngs = { "en" }, values = { "Explore" })
    String OpenDownloadFolderAction_actionPerformed_button_();

    @Default(lngs = { "en" }, values = { "Save" })
    String OpenDownloadFolderAction_actionPerformed_save_();

    @Default(lngs = { "en" }, values = { "Current Directory: %s1" })
    String OpenDownloadFolderAction_layoutDialogContent_current_(String absolutePath);

    @Default(lngs = { "en" }, values = { "Choose new Directory:" })
    String OpenDownloadFolderAction_layoutDialogContent_object_();

    @Default(lngs = { "en" }, values = { "%s1's Properties" })
    String ContextMenuFactory_createPopup_properties(String name);

    @Default(lngs = { "en" }, values = { "Set Comment(s)" })
    String SetCommentAction_SetCommentAction_object_();

    @Default(lngs = { "en" }, values = { "Edit Link/Package Comments" })
    String SetCommentAction_actionPerformed_dialog_title_();

    @Default(lngs = { "en" }, values = { "Custom Speedlimit" })
    String SpeedLimitator_SpeedLimitator_object_();

    @Default(lngs = { "en" }, values = { "Windows" })
    String AddonsMenu_updateMenu_windows_();

    @Default(lngs = { "en" }, values = { "Import Proxies" })
    String ProxyConfig_actionPerformed_import_title_();

    @Default(lngs = { "en" }, values = { "Enter one or more Proxyserver(s). \r\nFormat: (socks4|socks5|http)://[<user>:<pass>]@<host>:<port> - line seperated" })
    String ProxyConfig_actionPerformed_import_proxies_explain_();

    @Default(lngs = { "en" }, values = { "Export Proxies" })
    String ProxyTable_copy_export_title_();

    @Default(lngs = { "en" }, values = { "Socks 4 Proxy" })
    String gui_column_proxytype_socks4();

    @Default(lngs = { "en" }, values = { "Add a Premium Account" })
    String BuyAndAddPremiumAccount_BuyAndAddPremiumAccount_title_();

    @Default(lngs = { "en" }, values = { "JDownloader downloads as fast as possible.\r\nHowever, %s1 seems to limit the speed in free mode.\r\nTo download without interruptions and full speed, we recommend to use %s1's Premium Mode." })
    String SpeedColumn_getDescription_object_(String tld);

    @Default(lngs = { "en" }, values = { "Speed Alarm for %s1" })
    String SpeedColumn_onSingleClick_object_(String host);

    @Default(lngs = { "en" }, values = { "Finished" })
    String TaskColumn_getStringValue_finished_();

    @Default(lngs = { "en" }, values = { "Running" })
    String TaskColumn_getStringValue_running_();

    @Default(lngs = { "en" }, values = { "Current IP is blocked by %s1" })
    String TaskColumn_onSingleClick_object_(String host);

    @Default(lngs = { "en" }, values = { "%s1 blocked your current IP (Internet Address).\r\nIn many cases, JDownloader us able to do a Reconnect and get a new IP.\r\nIf this is not possible, we recommend to use %s1's Premium Mode." })
    String TaskColumn_getDescription_object_(String tld);

    @Default(lngs = { "en" }, values = { "Premium Alerts" })
    String literall_premium_alert();

    @Default(lngs = { "en" }, values = { "Matches on any File or Link and ignores conditions below" })
    String FilterRuleDialog_layoutDialogContent_lbl_always();

    @Default(lngs = { "en" }, values = { "Matches for any File or Link" })
    String BooleanFilter_toString_();

    @Default(lngs = { "en" }, values = { "Enter a Password if the Download is Password protected" })
    String AddLinksDialog_layoutDialogContent_downloadpassword_tt();

    @Default(lngs = { "en" }, values = { "Enter a Password for protected Links" })
    String AddLinksDialog_createExtracOptionsPanel_downloadpassword();

    @Default(lngs = { "en" }, values = { "Auto Extract" })
    String AddLinksDialog_layoutDialogContent_autoextract_lbl();

    @Default(lngs = { "en" }, values = { "Subfolder by Package" })
    String SetDownloadFolderInDownloadTableAction_modifiyNamePanel_package_();

    @Default(lngs = { "en" }, values = { "You really should know that..." })
    String literall_usage_tipp();

    @Default(lngs = { "en" }, values = { "Failed Links" })
    String DeleteFailedAction_DeleteFailedAction_object_();

    @Default(lngs = { "en" }, values = { "Successful Links" })
    String DeleteSuccessFulAction_DeleteSuccessFulAction_object_();

    @Default(lngs = { "en" }, values = { "DownloadLinks" })
    String DeleteAllAction_DeleteAllAction_object_();

    @Default(lngs = { "en" }, values = { "Offline Links" })
    String DeleteOfflineAction_DeleteOfflineAction_object_();

    @Default(lngs = { "en" }, values = { "Disabled Links" })
    String DeleteDisabledLinks_DeleteDisabledLinks_object_();

    @Default(lngs = { "en" }, values = { "Delete" })
    String DeleteQuickAction_DeleteQuickAction_object_();

    @Default(lngs = { "en" }, values = { "Clean Up" })
    String ContextMenuFactory_linkgrabber_createPopup_cleanup();

    @Default(lngs = { "en" }, values = { "Do you want to remove all Links of the Archive %s1 from Linkgrabber?" })
    String RemoveIncompleteArchives_run_(String name);

    @Default(lngs = { "en" }, values = { "Delete Selection" })
    String RemoveOptionsAction_actionPerformed_selection_();

    @Default(lngs = { "en" }, values = { "Folder does not exist!" })
    String DownloadFolderChooserDialog_handleNonExistingFolders_title_();

    @Default(lngs = { "en" }, values = { "The folder %s1\r\ndoes not exist. Do you want to create it?" })
    String DownloadFolderChooserDialog_handleNonExistingFolders_msg_(String path);

    @Default(lngs = { "en" }, values = { "Language" })
    String gui_config_language();

    @Default(lngs = { "en" }, values = { "Restart Required!" })
    String GUISettings_save_language_changed_restart_required_title();

    @Default(lngs = { "en" }, values = { "You have to restart JDownloader for a language change. Restart now?" })
    String GUISettings_save_language_changed_restart_required_msg();

    @Default(lngs = { "en" }, values = { "Nothing found!" })
    String AddLinksAction_actionPerformed_deep_title();

    @Default(lngs = { "en" }, values = { "JDownloader couldn't find any links to download on this website.\r\nWould you like to perform a deep link analysis to show all the files that can be downloaded from it?" })
    String AddLinksAction_actionPerformed_deep_msg();

    @Default(lngs = { "en" }, values = { "Download can be resumed" })
    String ConnectionColumn_DownloadIsResumeable();

    @Default(lngs = { "en" }, values = { "Account in use: %s1" })
    String ConnectionColumn_DownloadUsesAccount(String s);

    @Default(lngs = { "en" }, values = { "New Package" })
    String NewPackageDialog_NewPackageDialog_();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String NewPackageDialog_layoutDialogContent_newname_();

    @Default(lngs = { "en" }, values = { "Save to" })
    String NewPackageDialog_layoutDialogContent_saveto();

    @Default(lngs = { "en" }, values = { "Build Date:" })
    String jd_gui_swing_components_AboutDialog_builddate();

    @Default(lngs = { "en" }, values = { "Source Revisions" })
    String jd_gui_swing_components_AboutDialog_sourcerevisions();

    @Default(lngs = { "en" }, values = { "Core:" })
    String jd_gui_swing_components_AboutDialog_core();

    @Default(lngs = { "en" }, values = { "AppWork Utilities:" })
    String jd_gui_swing_components_AboutDialog_appworkutilities();

    @Default(lngs = { "en" }, values = { "Browser:" })
    String jd_gui_swing_components_AboutDialog_browser();

    @Default(lngs = { "en" }, values = { "Updater:" })
    String jd_gui_swing_components_AboutDialog_updater();

    @Default(lngs = { "en" }, values = { "Synthetica License Registration Number %s1" })
    String jd_gui_swing_components_AboutDialog_synthetica(String string);

    @Default(lngs = { "en" }, values = { "Filter" })
    String LinkgrabberFilter_initComponents_filter__title();

    @Default(lngs = { "en" }, values = { "Views" })
    String LinkgrabberFilter_initComponents_exceptions_title();

    @Default(lngs = { "en" }, values = { "Create a Log" })
    String LogAction();

    @Default(lngs = { "en" }, values = { "Create a Log" })
    String LogAction_tooltip();

    @Default(lngs = { "en" }, values = { "Please send this log ID to your supporter!" })
    String LogAction_actionPerformed_givelogid_();

    @Default(lngs = { "en" }, values = { "Checksum" })
    String checksumcolumnmd5();

    @Default(lngs = { "en" }, values = { "If there ever has been any warrenty for anything, it ends here!" })
    String AdvancedSettings_onShow_title_();

    @Default(lngs = { "en" }, values = { "These advanced settings should not be modified unless you know what you are doing. \r\nChanging them may affect stability, security and performance of JDownloader.\r\nYou have been warned." })
    String AdvancedSettings_onShow_msg_();

    @Default(lngs = { "en" }, values = { "Filter Settings" })
    String AdvancedSettings_AdvancedSettings_filter_();

    @Default(lngs = { "en" }, values = { "Key" })
    String AdvancedTableModel_initColumns_key_();

    @Default(lngs = { "en" }, values = { "Type" })
    String AdvancedTableModel_initColumns_type_();

    @Default(lngs = { "en" }, values = { "Value" })
    String AdvancedValueColumn_AdvancedValueColumn_object_();

    @Default(lngs = { "en" }, values = { "Valid Range: %s1 - %s2" })
    String RangeValidator_toString_object_(long min, long max);

    @Default(lngs = { "en" }, values = { "We need your help" })
    String JDGui_showStatsDialog_title_();

    @Default(lngs = { "en" }, values = { "To get a even better user experience in JDownloader 2,\r\nwe need some information about how you use JDownloader, which features you like, and which not.\r\nTo get this information, JDownloader will collect anonymous usage statistics. We understand that there might be users that do not want this. \r\nYou can disable the Stats Manager in the Advanced Settings at any time.\r\n\r\nIf you have any questions about this, feel free to contact our Developer Team in our Support Chat. " })
    String JDGui_showStatsDialog_message_();

    @Default(lngs = { "en" }, values = { "I want to help" })
    String JDGui_showStatsDialog_yes_();

    @Default(lngs = { "en" }, values = { "No, thanks" })
    String JDGui_showStatsDialog_no_();

    @Default(lngs = { "en" }, values = { "App Manager" })
    String ExtensionManager_getTitle_();

    @Default(lngs = { "en" }, values = { "Extensions and additional Apps that make JDownloader even better can be found here." })
    String ExtensionManager_ExtensionManager_description_();

    @Default(lngs = { "en" }, values = { "Search here..." })
    String pluginsettings_search_helptext();

    @Default(lngs = { "en" }, values = { "Please wait. Loading all plugins..." })
    String PluginSettingsPanel_PluginSettingsPanel_waittext_();

    @Default(lngs = { "en" }, values = { "Host Plugin: %s1" })
    String PluginSettingsPanel_runInEDT_plugin_header_text_host(String displayName);

    @Default(lngs = { "en" }, values = { "Crawler Plugin: %s1" })
    String PluginSettingsPanel_runInEDT_plugin_header_text_decrypt(String displayName);

    @Default(lngs = { "en" }, values = { "%s1 Crawler" })
    String PluginSettingsPanel_getListCellRendererComponent_crawler_(String displayName);

    @Default(lngs = { "en" }, values = { "Install" })
    String ExtensionModule_ExtensionModule_install_();

    @Default(lngs = { "en" }, values = { "Uninstall" })
    String ExtensionModule_ExtensionModule_uninstall_();

    @Default(lngs = { "en" }, values = { "%s1" })
    String ExtensionModule_createTitle_installed(String title);

    @Default(lngs = { "en" }, values = { "%s1" })
    String ExtensionModule_createTitle_not_installed(String title);

    @Default(lngs = { "en" }, values = { "Restart Required!" })
    String ExtensionModule_runInEDT_restart_required_();

    @Default(lngs = { "en" }, values = { "Installing..." })
    String ExtensionModule_run_installing_();

    @Default(lngs = { "en" }, values = { "Uninstalling..." })
    String ExtensionModule_run_uninstalling();

    @Default(lngs = { "en" }, values = { "Experimental & BETA Extensions - Use at your own risk!" })
    String ExtensionManager_getPanel_experimental_header_();

    @Default(lngs = { "en" }, values = { "Official Extensions - Get more out of JDownloader" })
    String ExtensionManager_getPanel_extensions_header_();

    @Default(lngs = { "en" }, values = { "3rd Party Apps" })
    String ExtensionManager_getPanel3rdparty_header_();

    @Default(lngs = { "en" }, values = { "Your App" })
    String Empty3rdPartyModule_getTitle_();

    @Default(lngs = { "en" }, values = { "If you created an Application for JDownloader and want us to offer it here - write us!" })
    String Empty3rdPartyModule_getDescription_();

    @Default(lngs = { "en" }, values = { "Reset Linkgrabber" })
    String ResetPopupAction_ResetPopupAction_();

    @Default(lngs = { "en" }, values = { "Reset Linkgrabber - Remove Links & reset Filters" })
    String ResetAction_ResetAction_tt();

    @Default(lngs = { "en" }, values = { "Reset Linkgrabber Options" })
    String ResetLinkGrabberOptionDialog_ResetLinkGrabberOptionDialog_title();

    @Default(lngs = { "en" }, values = { "Clear all Links & Packages" })
    String ResetLinkGrabberOptionDialog_layoutDialogContent_remove_links();

    @Default(lngs = { "en" }, values = { "Reset Table Sorter" })
    String ResetLinkGrabberOptionDialog_layoutDialogContent_sort();

    @Default(lngs = { "en" }, values = { "Clear Search Filter" })
    String ResetLinkGrabberOptionDialog_layoutDialogContent_search();

    @Default(lngs = { "en" }, values = { "Cancel pending Crawler jobs" })
    String ResetLinkGrabberOptionDialog_layoutDialogContent_interrup_crawler();

    @Default(lngs = { "en" }, values = { "Do you really want to reset the Linkgrabber? Please choose:" })
    String ResetLinkGrabberOptionDialog_layoutDialogContent_();

    @Default(lngs = { "en" }, values = { "Cannot reconnect: You are offline!" })
    String ReconnectInvoker_validate_offline_();

    @Default(lngs = { "en" }, values = { "Reconnect Failed" })
    String ReconnectDialog_failed();

    @Default(lngs = { "en" }, values = { "Launcher:" })
    String jd_gui_swing_components_AboutDialog_launcher();

    @Default(lngs = { "en" }, values = { "Could not create Folder %s1" })
    String DownloadFolderChooserDialog_handleNonExistingFolders_couldnotcreatefolder(String folder);

    @Default(lngs = { "en" }, values = { "Reset selected entries to default values" })
    String AdvancedTable_onContextMenu_reset_selection();

    @Default(lngs = { "en" }, values = { "Are you sure?" })
    String lit_are_you_sure();

    @Default(lngs = { "en" }, values = { "Do you really want to reset %s1 advanced config entry/ies to their default values?" })
    String AdvancedTablecontextmenu_reset(int size);

    @Default(lngs = { "en" }, values = { "Description" })
    String AdvancedTableModel_initColumns_desc_();

    @Default(lngs = { "en" }, values = { "Duration" })
    String DurationColumn_DurationColumn_object_();

    @Default(lngs = { "en" }, values = { "Do you want to stop all running downloads?\r\nYou would loose %s1 due to %s2 non resumable Download/s!" })
    String StopDownloadsAction_run_msg_(String bytes, int i);

    @Default(lngs = { "en" }, values = { "Do you want to disable and stop running downloads?\r\nYou would loose %s1 due to %s2 non resumable Download/s!" })
    String EnableAction_run_msg_(String bytes, int i);

    @Default(lngs = { "en" }, values = { "Do you want to skip and stop running  downloads?\r\nYou would loose %s1 due to %s2 non resumable Download/s!" })
    String SkipAction_run_msg_(String bytes, int i);

    @Default(lngs = { "en" }, values = { "No" })
    String lit_no();

    @Default(lngs = { "en" }, values = { "Yes" })
    String lit_yes();

    @Default(lngs = { "en" }, values = { "dd.MM.yy" })
    String PremiumAccountTableModel_getDateFormatString_();

    @Default(lngs = { "en" }, values = { "Download Overview" })
    String OverViewHeader_OverViewHeader_();

    @Default(lngs = { "en" }, values = { "Package(s):" })
    String DownloadOverview_DownloadOverview_packages();

    @Default(lngs = { "en" }, values = { "Link(s):" })
    String DownloadOverview_DownloadOverview_links();

    @Default(lngs = { "en" }, values = { "Reset Dialog 'Don't show again' flags" })
    String GUISettings_GUISettings_resetdialogs_();

    @Default(lngs = { "en" }, values = { "Dialog 'Don't show again' are resetted now!" })
    String GUISettings_actionPerformed_reset_done();

    @Default(lngs = { "en" }, values = { "Dialog Windows" })
    String gui_config_dialogs();

    @Default(lngs = { "en" }, values = { "Please wait. Loading Links..." })
    String DownloadsTable_DownloadsTable_object_wait_for_loading_links();

    @Default(lngs = { "en" }, values = { "Please wait. Loading Links..." })
    String LinkGrabberTable_LinkGrabberTable_object_wait_for_loading_links();

    @Default(lngs = { "en" }, values = { "Skip" })
    String AbstractCaptchaDialog_AbstractCaptchaDialog_cancel();

    @Default(lngs = { "en" }, values = { "Send" })
    String AbstractCaptchaDialog_AbstractCaptchaDialog_continue();

    @Default(lngs = { "en" }, values = { "Stop all Downloads" })
    String AbstractCaptchaDialog_createPopup_skip_and_stop_all_downloads();

    @Default(lngs = { "en" }, values = { "Skip all captchas for host '%s1'" })
    String AbstractCaptchaDialog_createPopup_skip_and_disable_all_downloads_from(String host);

    @Default(lngs = { "en" }, values = { "Skip all captchas for Package '%s1'" })
    String AbstractCaptchaDialog_createPopup_skip_and_disable_package(String packageName);

    @Default(lngs = { "en" }, values = { "Skip all captchas" })
    String AbstractCaptchaDialog_createPopup_skip_and_hide_all_captchas_download();

    @Default(lngs = { "en" }, values = { "Skipped Downloads!" })
    String StatusBarImpl_skippedLinksMarker_title();

    @Default(lngs = { "en" }, values = { "Skipped Downloads: %s1. Click here to retry them." })
    String StatusBarImpl_skippedLinksMarker_desc(int i);

    @Default(lngs = { "en" }, values = { "Download has been skipped. Restart Downloads to try again!" })
    String ConnectionColumn_DownloadIsSkipped();

    @Default(lngs = { "en" }, values = { "Refresh" })
    String CaptchaDialog_layoutDialogContent_refresh();

    @Default(lngs = { "en" }, values = { "Links have been skipped" })
    String ChallengeDialogHandler_viaGUI_skipped_help_title();

    @Default(lngs = { "en" }, values = { "Congrats! You found the 'Skip Captcha Downloads'-Feature!\r\nOne or more Downloadlinks have been skipped to avoid further Captcha Dialogs.\r\nIf you want to Download these links anyway, there are three options:\r\n\r\n  1. Restart Downloads\r\n  2. Rightclick -> More ->Reset Downloadlink\r\n  3. Rightclick -> Start Download" })
    String ChallengeDialogHandler_viaGUI_skipped_help_msg();

    @Default(lngs = { "en" }, values = { "Rebuilding Plugincache..." })
    String DownloadsTable_DownloadsTable_init_plugins();

    @Default(lngs = { "en" }, values = { "1. Choose a Hoster" })
    String AddAccountDialog_layoutDialogContent_choosehoster_();

    @Default(lngs = { "en" }, values = { "2. Enter your Acount's Login Information" })
    String AddAccountDialog_layoutDialogContent_enterlogininfo();

    @Default(lngs = { "en" }, values = { "Save" })
    String lit_save();

    @Default(lngs = { "en" }, values = { "The %s1 plugin is outdated. Please run an update!" })
    String AddAccountDialog_actionPerformed_outdated(String host);

    @Default(lngs = { "en" }, values = { "or Premium Key" })
    String LetitBitAccountFactory_LetitBitPanel_key();

    @Default(lngs = { "en" }, values = { "Enter a premiumkey instead of username +  password..." })
    String LetitBitAccountFactory_LetitBitPanel_key_help();

    @Default(lngs = { "en" }, values = { "Cancel" })
    String lit_cancel();

    @Default(lngs = { "en" }, values = { "Cancel & Stop further Crawling" })
    String AbstractCaptchaDialog_createPopup_cancel_linkgrabbing();

    @Default(lngs = { "en" }, values = { "Cancel, but continue without asking for Captchas" })
    String AbstractCaptchaDialog_createPopup_cancel_stop_showing_crawlercaptchs();

    @Default(lngs = { "en" }, values = { "Total Bytes:" })
    String DownloadOverview_DownloadOverview_size();

    @Default(lngs = { "en" }, values = { "Bytes loaded:" })
    String DownloadOverview_DownloadOverview_loaded();

    @Default(lngs = { "en" }, values = { "Downloadspeed:" })
    String DownloadOverview_DownloadOverview_speed();

    @Default(lngs = { "en" }, values = { "ETA:" })
    String DownloadOverview_DownloadOverview_eta();

    @Default(lngs = { "en" }, values = { "Choose Information to display" })
    String OverViewHeader_OverViewHeader_settings_tooltip_();

    @Default(lngs = { "en" }, values = { "Total Information - all Links in the List" })
    String OverViewHeader_actionPerformed_total_();

    @Default(lngs = { "en" }, values = { "Visible Links Information - do not include filtered Links" })
    String OverViewHeader_actionPerformed_visible_only_();

    @Default(lngs = { "en" }, values = { "Selected Links Information - only include selected Links & Packages" })
    String OverViewHeader_actionPerformed_selected_();

    @Default(lngs = { "en" }, values = { "Total Information - all Links in the List" })
    String DownloadOverview_DownloadOverview_tooltip1();

    @Default(lngs = { "en" }, values = { "Visible Links Information - do not include filtered Links" })
    String DownloadOverview_DownloadOverview_tooltip2();

    @Default(lngs = { "en" }, values = { "Selected Links Information - only include selected Links & Packages" })
    String DownloadOverview_DownloadOverview_tooltip3();

    @Default(lngs = { "en" }, values = { "Smart Information - Depends on your selection & filter settings" })
    String OverViewHeader_actionPerformed_smart_();

    @Default(lngs = { "en" }, values = { "Show Quicksettings" })
    String OverViewHeader_actionPerformed_quicksettings();

    @Default(lngs = { "en" }, values = { "Running Downloads" })
    String DownloadOverview_DownloadOverview_running_downloads();

    @Default(lngs = { "en" }, values = { "Open Connections" })
    String DownloadOverview_DownloadOverview_connections();

    @Default(lngs = { "en" }, values = { "Do you want to delete all visible files in your Downloadlist? Be wise -  Once confirmed, this cannot be undone!" })
    String ClearDownloadListAction_actionPerformed_();

    @Default(lngs = { "en" }, values = { "Do you want to delete all selected Downloadlinks? Be wise - Once confirmed, this cannot be undone!" })
    String RemoveSelectionAction_actionPerformed_();

    @Default(lngs = { "en" }, values = { "Do you want to delete all not selected Downloadlinks? Be wise - Once confirmed, this cannot be undone!" })
    String RemoveNonSelectedAction_actionPerformed();

    @Default(lngs = { "en" }, values = { "Do you want to remove all offline Downloadlinks? Be wise - Once confirmed, this cannot be undone!" })
    String RemoveOfflineAction_actionPerformed();

    @Default(lngs = { "en" }, values = { "Start Downloads" })
    String ForceDownloadAction_actionPerformed_help_title_();

    @Default(lngs = { "en" }, values = { "...you just forced JDownloader to start the selected Link(s) as soon as possible.\r\nThis ignores all limitations you set up (e.g. Maximum simultane Downloads).\r\nIf you just want to start all Downloads from top to bottom, you should use the 'Start Downloads' Button (The Playback Icon) in the Toolbar above." })
    String ForceDownloadAction_actionPerformed_help_msg_();

    @Default(lngs = { "en" }, values = { "Download is waiting for a prioritized start" })
    String ConnectionColumn_DownloadIsForced();

    @Default(lngs = { "en" }, values = { "Clear Linkgrabber" })
    String RemoveAllLinkgrabberAction_RemoveAllLinkgrabberAction_object_();

    @Default(lngs = { "en" }, values = { "Remove selected Links from Linkgrabber" })
    String RemoveSelectionLinkgrabberAction_RemoveSelectionLinkgrabberAction_object_();

    @Default(lngs = { "en" }, values = { "Force Download Start" })
    String ForceDownloadAction_ForceDownloadAction();

    @Default(lngs = { "en" }, values = { "Skip Download" })
    String ForceDownloadAction_SkipDownloadAction();

    @Default(lngs = { "en" }, values = { "Unskip Download" })
    String ForceDownloadAction_UnskipDownloadAction();

    @Default(lngs = { "en" }, values = { "Move downloaded Files to Trash & remove Links from JDownloader" })
    String ConfirmDeleteLinksDialog_layoutDialogContent_Recycle_2();

    @Default(lngs = { "en" }, values = { "Delete downloaded Files permanently from harddisk & remove Links from JDownloader" })
    String ConfirmDeleteLinksDialog_layoutDialogContent_delete_2();

    @Default(lngs = { "en" }, values = { "Keep all downloaded Files - just remove Links from JDownloader" })
    String ConfirmDeleteLinksDialog_layoutDialogContent_no_filedelete2();

    @Default(lngs = { "en" }, values = { "\r\n%s1 Links(s) to delete\r\n%s4 Files(%s2) on Disk\r\nLinks left in Downloadlist: %s3" })
    String DeleteSelectionAction_actionPerformed_affected2(int linkCount, String bytes, int linksLeft, int localFileCount);

    @Default(lngs = { "en" }, values = { "Do you want to delete all selected & disabled Downloadlinks?" })
    String DeleteDisabledLinksFromListAndDiskAction_actionPerformed_object_();

    @Default(lngs = { "en" }, values = { "Do you want to delete all selected & failed Downloadlinks?" })
    String DeleteFailedFromListAndDiskAction_actionPerformed();

    @Default(lngs = { "en" }, values = { "Do you want to delete all selected & unavailable Downloadlinks?" })
    String DeleteSelectedOfflineLinksAction_actionPerformed();

    @Default(lngs = { "en" }, values = { "Ups... something is wrong!" })
    String lit_ups_something_is_wrong();

    @Default(lngs = { "en" }, values = { "There are no Downloadlinks to delete!" })
    String DownloadController_deleteLinksRequest_nolinks();

    @Default(lngs = { "en" }, values = { "Delete" })
    String lit_delete();

    @Default(lngs = { "en" }, values = { "Do you want to delete all selected & successfully downloaded Downloadlinks?" })
    String DeleteSelectedFinishedLinksAction_actionPerformed();

    @Default(lngs = { "en" }, values = { "Right Click Menu: Download Table" })
    String gui_config_menumanager_downloadlist();

    @Default(lngs = { "en" }, values = { "Right Click Menu: Linkgrabber Table" })
    String gui_config_menumanager_linkgrabber();

    @Default(lngs = { "en" }, values = { "You can customize many menus in JDownloader - Main Toolbar, Context menus,..." })
    String gui_config_menumanager_desc();

    @Default(lngs = { "en" }, values = { "Open" })
    String lit_open();

    @Default(lngs = { "en" }, values = { "Delete selected" })
    String RemoveOptionsAction_actionPerformed_selected();

    @Default(lngs = { "en" }, values = { "More Cleanup and Delete Options..." })
    String RemoveOptionsAction_RemoveOptionsAction_tt();

    @Default(lngs = { "en" }, values = { "Advanced Options to add Links..." })
    String AddOptionsAction_AddOptionsAction_tt();

    @Default(lngs = { "en" }, values = { "Add new Links to download..." })
    String AddLinksAction_AddLinksAction_tt();

    @Default(lngs = { "en" }, values = { "Menu Customizer: %s1" })
    String ManagerFrame_ManagerFrame_title(String name);

    @Default(lngs = { "en" }, values = { "Sort package by Column" })
    String SortAction_SortAction_object_empty();

    @Default(lngs = { "en" }, values = { "Enable/Disable Link" })
    String EnabledAction_EnabledAction_empty();

    @Default(lngs = { "en" }, values = { "Set/Remove Stopmark" })
    String gui_table_contextmenu_stopmark();

    @Default(lngs = { "en" }, values = { "----------------------------" })
    String Renderer_getTreeCellRendererComponent_seperator();

    @Default(lngs = { "en" }, values = { "Edit Menu Structure (Use Drag&Drop)" })
    String ManagerFrame_layoutPanel_menustructure_();

    @Default(lngs = { "en" }, values = { "Edit Menu Node" })
    String ManagerFrame_layoutPanel_info();

    @Default(lngs = { "en" }, values = { "Add Action" })
    String ManagerFrame_layoutPanel_add();

    @Default(lngs = { "en" }, values = { "Submenu" })
    String InfoPanel_update_submenu();

    @Default(lngs = { "en" }, values = { "Link" })
    String InfoPanel_update_link();

    @Default(lngs = { "en" }, values = { "Action" })
    String InfoPanel_update_action();

    @Default(lngs = { "en" }, values = { "Properties" })
    String InfoPanel_InfoPanel_properties_();

    @Default(lngs = { "en" }, values = { "Hide if Downloads are running" })
    String InfoPanel_InfoPanel_hideIfDownloadesRunning();

    @Default(lngs = { "en" }, values = { "Hide if Downloads aren't running" })
    String InfoPanel_InfoPanel_hideIfDownloadsNotRunning();

    @Default(lngs = { "en" }, values = { "Hide if Action is disabled" })
    String InfoPanel_InfoPanel_hideIfDisabled();

    @Default(lngs = { "en" }, values = { "Hide if System does not Support opening Files" })
    String InfoPanel_InfoPanel_hideIfOpenFileIsUnsupported();

    @Default(lngs = { "en" }, values = { "Hide if File/Folder does not exist" })
    String InfoPanel_InfoPanel_hideIfFileNotExists();

    @Default(lngs = { "en" }, values = { "Hide if clicked on a Package" })
    String InfoPanel_InfoPanel_linkContext2();

    @Default(lngs = { "en" }, values = { "Hide if clicked on a Link" })
    String InfoPanel_InfoPanel_packageContext2();

    @Default(lngs = { "en" }, values = { "Ahh... a new dialog?" })
    String DownloadController_deleteLinksRequest_object_help();

    @Default(lngs = { "en" }, values = { "oh - it seems that you just saw the 'Are you sure...' Dialog! You might think 'OMG what a useless dialog' now.\r\nIf you do, and do not want to see this dialog again, you have two options:\r\n     1. Press the Ctrl Key while Deleting links - this will bypass the dialog.\r\n     2. Go to Advanced Options and disable the dialog forever" })
    String DownloadController_deleteLinksRequest_object_msg();

    @Default(lngs = { "en" }, values = { "Choose Action" })
    String ManagerFrame_actionPerformed_addaction_title();

    @Default(lngs = { "en" }, values = { "Please Choose the action you want to add!" })
    String ManagerFrame_actionPerformed_addaction_msg();

    @Default(lngs = { "en" }, values = { "Add Submenu" })
    String ManagerFrame_layoutPanel_addSubmenu();

    @Default(lngs = { "en" }, values = { "Add Special Entry" })
    String ManagerFrame_layoutPanel_addspecials();

    @Default(lngs = { "en" }, values = { "Create a custom Submenu" })
    String NewSubMenuDialog_NewSubMenuDialog_title();

    @Default(lngs = { "en" }, values = { "Choose Icon and Name..." })
    String NewSubMenuDialog_layoutDialogContent_name_();

    @Default(lngs = { "en" }, values = { "Open Menu Manager" })
    String MenuManagerAction_MenuManagerAction();

    @Default(lngs = { "en" }, values = { "Predefined: \"%s1\"" })
    String AddSubMenuAction_getListCellRendererComponent(String name);

    @Default(lngs = { "en" }, values = { "---- Seperator Line ----" })
    String AddSpecialAction_actionPerformed_seperator();

    @Default(lngs = { "en" }, values = { "Choose Menu Item" })
    String AddSpecialAction_actionPerformed_title();

    @Default(lngs = { "en" }, values = { "Please choose what item you want to add!" })
    String AddSpecialAction_actionPerformed_msg();

    @Default(lngs = { "en" }, values = { "Add" })
    String lit_add();

    @Default(lngs = { "en" }, values = { "Reset to Default" })
    String ManagerFrame_layoutPanel_resettodefault();

    @Default(lngs = { "en" }, values = { "Change Icon" })
    String InfoPanel_changeicon();

    @Default(lngs = { "en" }, values = { "(%s2) %s1" })
    String InfoPanel_updateInfo_header_actionlabel(String name, String infoPanel_update_action);

    @Default(lngs = { "en" }, values = { "Custom name" })
    String InfoPanel_InfoPanel_itemname_();

    @Default(lngs = { "en" }, values = { "Custom Icon" })
    String InfoPanel_InfoPanel_icon();

    @Default(lngs = { "en" }, values = { "Add %s1" })
    String AddGenericItem_AddGenericItem_(String name);

    @Default(lngs = { "en" }, values = { "Seperator" })
    String SeperatorData_SeperatorData();

    @Default(lngs = { "en" }, values = { "Always hidden" })
    String InfoPanel_InfoPanel_hidden();

    @Default(lngs = { "en" }, values = { "export" })
    String lit_export();

    @Default(lngs = { "en" }, values = { "import" })
    String lit_import();

    @Default(lngs = { "en" }, values = { "Export the current Menu Structure" })
    String ManagerFrame_actionPerformed_export_title();

    @Default(lngs = { "en" }, values = { "An Error occured..." })
    String lit_error_occured();

    @Default(lngs = { "en" }, values = { "Use Account(s) to download" })
    String AccountManagerSettings_AccountManagerSettings_disable_global();

    @Default(lngs = { "en" }, values = { "Main Toolbar" })
    String gui_config_menumanager_toolbar();

    @Default(lngs = { "en" }, values = { "All Actions are already on use in the selected submenu" })
    String AddActionAction_getListCellRendererComponent_no_action_();

    @Default(lngs = { "en" }, values = { "Downloads list - Rightclick menu" })
    String DownloadListContextMenuManager_getName();

    @Default(lngs = { "en" }, values = { "LinkGrabber list - Rightclick menu" })
    String LinkgrabberContextMenuManager_getName();

    @Default(lngs = { "en" }, values = { "Main Toolbar" })
    String MainToolbarManager_getName();

    @Default(lngs = { "en" }, values = { "%s1" })
    String ExtensionQuickToggleAction_name_selected2(String name);

    @Default(lngs = { "en" }, values = { "%s1" })
    String ExtensionQuickToggleAction_name_deselected2(String name);

    @Default(lngs = { "en" }, values = { "Disable  the '%s1' Extension" })
    String ExtensionQuickToggleAction_name_selected_tt(String name);

    @Default(lngs = { "en" }, values = { "Enable the '%s1' Extension" })
    String ExtensionQuickToggleAction_name_deselected_tt(String name);

    @Default(lngs = { "en" }, values = { "Move to Top" })
    String MoveToTopAction_MoveToTopAction();

    @Default(lngs = { "en" }, values = { "Move up" })
    String MoveUpAction_MoveUpAction();

    @Default(lngs = { "en" }, values = { "Move to Bottom" })
    String MoveToBottomAction_MoveToBottomAction();

    @Default(lngs = { "en" }, values = { "Move down" })
    String MoveDownAction_MoveDownAction();

    @Default(lngs = { "en" }, values = { "Main Menu" })
    String gui_config_menumanager_mainmenu();

    @Default(lngs = { "en" }, values = { "Main Menu" })
    String MainMenuManager_getName();

    @Default(lngs = { "en" }, values = { "Custom Shortcut" })
    String InfoPanel_InfoPanel_shortcuts();

    @Default(lngs = { "en" }, values = { "Click here & press a shortcut..." })
    String InfoPanel_InfoPanel_shortcuthelp();

    @Default(lngs = { "en" }, values = { "Enter a name for this action..." })
    String InfoPanel_InfoPanel_customname_help();

    @Default(lngs = { "en" }, values = { "Rename" })
    String RenameAction_RenameAction();

    @Default(lngs = { "en" }, values = { "Rename the currently selected Link or Package" })
    String RenameAction_RenameAction_tt();

    @Default(lngs = { "en" }, values = { "User Interface" })
    String GUISettings_getTitle();

    @Default(lngs = { "en" }, values = { "Customize JDownloader -  Mofify the User Interface, Menus, Language, ... whatever" })
    String GUISettings_GUISettings_description();

    @Default(lngs = { "en" }, values = { "Accessibility" })
    String GUISettings_GUISettings_object_accessability();

    @Default(lngs = { "en" }, values = { "Tray Menu" })
    String gui_config_menumanager_traymenu();

    @Default(lngs = { "en" }, values = { "Enable & disable the Silent Mode - Avoid Popups" })
    String action_silentmode_tooltip();

    @Default(lngs = { "en" }, values = { "Restart required for %s1" })
    String AdvancedConfigEntry_setValue_restart_warning_title(String key);

    @Default(lngs = { "en" }, values = { "You changed the %s1-option. You have to restart JDownloader to reinitialize this option." })
    String AdvancedConfigEntry_setValue_restart_warning(String key);

    @Default(lngs = { "en" }, values = { "Enable/Disable all Captcha Exchange Services  (like 9kw, Captchabrotherhood, ...)" })
    String CaptchaExchangeToogleAction_createTooltip_();

    @Default(lngs = { "en" }, values = { "Enable/Disable JAC - the JDownloader Auto Captcha Solver" })
    String JAntiCaptchaToogleAction_createTooltip_();

    @Default(lngs = { "en" }, values = { "Enable/Disable Remote Anti Captcha (Like Mobile Apps, My.JDownloader,..." })
    String RemoteCaptchaToogleAction_createTooltip_();

    @Default(lngs = { "en" }, values = { "Enable/Disable Captcha Dialogs. (Check our SilentMode)" })
    String CaptchaDialogsToogleAction_createTooltip_();

    @Default(lngs = { "en" }, values = { "Auto Reconnect" })
    String AutoReconnectToggleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Auto Reconnect" })
    String AutoReconnectToggleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Captcha Dialogs" })
    String CaptchaDialogsToogleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Captcha Dialogs" })
    String CaptchaDialogsToogleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Captcha Exchange Services" })
    String CaptchaExchangeToogleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Captcha Exchange Services" })
    String CaptchaExchangeToogleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Clipboard Monitoring" })
    String ClipBoardToggleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Clipboard Monitoring" })
    String ClipBoardToggleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Premium Downloads" })
    String GlobalPremiumSwitchToggleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Premium Downloads" })
    String GlobalPremiumSwitchToggleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Auto Captcha Recognition" })
    String JAntiCaptchaToogleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Auto Captcha Recognition" })
    String JAntiCaptchaToogleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Captchas via My.JDownloader" })
    String RemoteCaptchaToogleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Captchas via My.JDownloader" })
    String RemoteCaptchaToogleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "Silent Mode" })
    String SilentModeToggleAction_getNameWhenDisabled_();

    @Default(lngs = { "en" }, values = { "Silent Mode" })
    String SilentModeToggleAction_getNameWhenEnabled_();

    @Default(lngs = { "en" }, values = { "More..." })
    String premiumaccounttablemodel_column_info();

    @Default(lngs = { "en" }, values = { "Show Details" })
    String premiumaccounttablemodel_column_info_button();

    @Default(lngs = { "en" }, values = { "Menus and Toolbars" })
    String gui_config_menumanager_header();

    @Default(lngs = { "en" }, values = { "General Reconnect Options" })
    String ReconnectSettings_ReconnectSettings_settings_();

    @Default(lngs = { "en" }, values = { "General Reconnect related options can be found here." })
    String ReconnectSettings_ReconnectSettings_settings_desc();

    @Default(lngs = { "en" }, values = { "Auto Reconnect Enabled" })
    String ReconnectSettings_ReconnectSettings_enabled_();

    @Default(lngs = { "en" }, values = { "Do not start downloads if others wait for a Reconnect" })
    String ReconnectSettings_ReconnectSettings_prefer_reconnect_desc();

    @Default(lngs = { "en" }, values = { "Priorize Reconnect" })
    String ReconnectSettings_ReconnectSettings_prefer_reconnect();

    @Default(lngs = { "en" }, values = { "Reconnects can interrupt resumable downloads" })
    String ReconnectSettings_ReconnectSettings_interrupt_resumable_allowed();

    @Default(lngs = { "en" }, values = { "Enter a comment or leave empty..." })
    String AddLinksDialog_layoutDialogContent_comment_help();

    @Default(lngs = { "en" }, values = { "Enter a comment. This comment will be stored in every package and downloadlink." })
    String AddLinksDialog_layoutDialogContent_comment_tt();

    @Default(lngs = { "en" }, values = { "Please choose a plugin: " })
    String PluginSettingsPanel_runInEDT_choose_();

    @Default(lngs = { "en" }, values = { "Reset Window Positions and Dimensions" })
    String GUISettings_GUISettings_resetdialog_positions_();

    @Default(lngs = { "en" }, values = { "Native Authentication" })
    String gui_column_nativeauth();

    @Default(lngs = { "en" }, values = { "Default Connection" })
    String ProxyConfig_ProxyConfig_defaultproxy_();

    @Default(lngs = { "en" }, values = { "Found Proxies: %s1" })
    String ProxyAutoAction_run_added_proxies_(int size);

    @Default(lngs = { "en" }, values = { "Horizontal Scrollbar" })
    String HorizontalScrollbarAction_columnControlMenu_scrollbar_();

    @Default(lngs = { "en" }, values = { "Overwrite file?" })
    String lit_file_exists();

    @Default(lngs = { "en" }, values = { "The file %s1 already exists.\r\nDo you want to overwrite it?" })
    String lit_file_already_exists_overwrite_question(String absoluteFile);

    @Default(lngs = { "en" }, values = { "No file selected!" })
    String DLCFactory_createDLCByCrawledLinks_nofile_title();

    @Default(lngs = { "en" }, values = { "Please choose a valid path to save the DownloadLinkContainer (DLC)." })
    String DLCFactory_createDLCByCrawledLinks_nofile_msg();

    @Default(lngs = { "en" }, values = { "DLC creation Sucessful" })
    String DLCFactory_writeDLC_success_ok();

    @Default(lngs = { "en" }, values = { "Open Path" })
    String DLCFactory_writeDLC_showpath();

    @Default(lngs = { "en" }, values = { "Close" })
    String lit_close();

    @Default(lngs = { "en" }, values = { "Remove selected item" })
    String ManagerFrame_layoutPanel_remove();

    @Default(lngs = { "en" }, values = { "Overview" })
    String LinkgrabberOverViewHeader_LinkgrabberOverViewHeader_();

    @Default(lngs = { "en" }, values = { "Aggregated Overview over all Links in the Linkgrabber" })
    String LinkgrabberOverViewHeader_LinkgrabberOverViewHeader_settings_tooltip_();

    @Default(lngs = { "en" }, values = { "Hoster(s):" })
    String DownloadOverview_DownloadOverview_hoster();

    @Default(lngs = { "en" }, values = { "Online:" })
    String DownloadOverview_DownloadOverview_online();

    @Default(lngs = { "en" }, values = { "Offline:" })
    String DownloadOverview_DownloadOverview_offline();

    @Default(lngs = { "en" }, values = { "Unknown Status:" })
    String DownloadOverview_DownloadOverview_unknown();

    @Default(lngs = { "en" }, values = { "Cannot modify this rule" })
    String PackagizerFilterRuleDialog_layoutDialogContent_cannot_modify_();

    @Default(lngs = { "en" }, values = { "Predifined rules cannot be modified" })
    String PackagizerFilter_valueChanged_disable_static();

    @Default(lngs = { "en" }, values = { "Predefined rule: %s1" })
    String FilterTableModel_initColumns_static_(String name);

    @Default(lngs = { "en" }, values = { "My.JDownloader" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_title_();

    @Default(lngs = { "en" }, values = { "Sign up for an account at my.jdownloader.org, and access a lot of premium features like a JDownloader Remotecontrol via a Webinterface for your Browser, or apps for Android, IPhone and WindowsPhone" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_description();

    @Default(lngs = { "en" }, values = { "Go to My.JDownloader.org" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_open_();

    @Default(lngs = { "en" }, values = { "My Account" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_logins_();

    @Default(lngs = { "en" }, values = { "Username/Email" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_username_();

    @Default(lngs = { "en" }, values = { "Password" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_password_();

    @Default(lngs = { "en" }, values = { "Account active" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_enabled();

    @Default(lngs = { "en" }, values = { "Your Account has been deactivated because your Email has not been confirmed yet. We sent you an Email to confirm your adress. Please click the Link in the Email." })
    String MyJDownloaderSettingsPanel_runInEDT_account_unconfirmed_();

    @Default(lngs = { "en" }, values = { "Your Account has been deactivated because an error occured: %s1" })
    String MyJDownloaderSettingsPanel_runInEDT_account_unknown(String string);

    @Default(lngs = { "en" }, values = { "Your Account has been deactivated because the entered Username/Email or Password is wrong." })
    String MyJDownloaderSettingsPanel_runInEDT_account_badlogins();

    @Default(lngs = { "en" }, values = { "Connection estabilished. Great!" })
    String MyJDownloaderSettingsPanel_runInEDT_connected_2();

    @Default(lngs = { "en" }, values = { "Current connections: %s1" })
    String MyJDownloaderSettingsPanel_runInEDT_connections(int connections);

    @Default(lngs = { "en" }, values = { "Connection pending. Please wait!" })
    String MyJDownloaderSettingsPanel_runInEDT_pending();

    @Default(lngs = { "en" }, values = { "Not Connected!" })
    String MyJDownloaderSettingsPanel_runInEDT_disconnected_();

    @Default(lngs = { "en" }, values = { "Connect" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_connect_();

    @Default(lngs = { "en" }, values = { "Disconnect" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_disconnect_();

    @Default(lngs = { "en" }, values = { "Auto connect" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_autoconnect_();

    @Default(lngs = { "en" }, values = { "Auto connect on JDownloader start" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_autoconnect_tt();

    @Default(lngs = { "en" }, values = { "Save Changes" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_reconnect_();

    @Default(lngs = { "en" }, values = { "Local Settings" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_local();

    @Default(lngs = { "en" }, values = { "My Account" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_remote();

    @Default(lngs = { "en" }, values = { "Information and configuration of your My.JDownloader Account" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_remote_description();

    @Default(lngs = { "en" }, values = { "Enter the device name of this JDownloader instance. You can manage different JDownloader instances in one My.JDownloader Account." })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_jd_name();

    @Default(lngs = { "en" }, values = { "Device Name" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_devicename_();

    @Default(lngs = { "en" }, values = { "Enter your My.JDownloader Logins below. If you do not have an account yet, click the Link above and register for an account. It's free!" })
    String MyJDownloaderSettingsPanel_MyJDownloaderSettingsPanel_jd_logins();

    @Default(lngs = { "en" }, values = { "%s1 - Updates available!" })
    String JDGui_updateTitle_updates_available(String string);

    @Default(lngs = { "en" }, values = { "Lock all Columns" })
    String LockAllColumnsAction_LockAllColumnsAction_lockall_columns_();

    @Default(lngs = { "en" }, values = { "Unlock all Columns" })
    String LockAllColumnsAction_LockAllColumnsAction_unlockall_columns_();

    @Default(lngs = { "en" }, values = { "Choose new Color:" })
    String AdvancedValueColumn_onSingleClick_colorchooser_title_();

    @Default(lngs = { "en" }, values = { "Choose a Color or enter the Color manually!" })
    String AdvancedValueColumn_onSingleClick_colorColumn_title();

    @Default(lngs = { "en" }, values = { "... you can press ALT while clicking on this column to enter the Color code manually!" })
    String AdvancedValueColumn_onSingleClick_colorColumn_help();

    @Default(lngs = { "en" }, values = { "...you can use dynamic variables or Regular Expression matches here.\r\nJust do a rightclick in the textfield to explorer the full power of the Packagizer" })
    String PackagizerFilterRuleDialog_layoutDialogContent_help_dynamic_variables();

    @Default(lngs = { "en" }, values = { "It's a shame to hide these features!" })
    String PackagizerFilterRuleDialog_mousePressed_help_title();

    @Default(lngs = { "en" }, values = { "Delete Selection" })
    String ToolbarDeleteAction_ToolbarDeleteAction_delete_Selection();

    @Default(lngs = { "en" }, values = { "Delete disabled selected Links" })
    String RemoveDisabledAction_RemoveDisabledAction_object_();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all selected and disabled link(s)?" })
    String RemoveDisabledAction_actionPerformed_msg();

    @Default(lngs = { "en" }, values = { "Menuitem is visible" })
    String InfoPanel_InfoPanel_hidden_2();

    @Default(lngs = { "en" }, values = { "Delete selected & disabled Links" })
    String DeleteDisabledSelectedLinksToolbarAction_object_();

    @Default(lngs = { "en" }, values = { "disabled" })
    String lit_disabled();

    @Default(lngs = { "en" }, values = { "failed" })
    String lit_failed();

    @Default(lngs = { "en" }, values = { "offline" })
    String lit_offline();

    @Default(lngs = { "en" }, values = { "finished" })
    String lit_finished();

    @Default(lngs = { "en" }, values = { "Delete Selected Links" })
    String GenericDeleteSelectedToolbarAction_updateName_object_selected_all();

    @Default(lngs = { "en" }, values = { "Delete Links: Selected & " })
    String GenericDeleteSelectedToolbarAction_updateName_object_selected();

    @Default(lngs = { "en" }, values = { "Load Proxy Setup" })
    String SetProxySetupAction_SetProxySetupAction_();

    @Default(lngs = { "en" }, values = { "Export Proxy Setup" })
    String SaveAsProxyProfileAction_SaveAsProxyProfileAction_();

    @Default(lngs = { "en" }, values = { "Export Proxy Setup" })
    String ImportPlainTextAction_ImportPlainTextAction_();

    @Default(lngs = { "en" }, values = { "Load Proxy Setup" })
    String LoadProxyProfileAction_LoadProxyProfileAction_();

    @Default(lngs = { "en" }, values = { "Save Proxy Setup" })
    String SaveAsProxyProfileAction_actionPerformed_choose_file();

    @Default(lngs = { "en" }, values = { "Load Proxy Setup" })
    String LoadProxyProfileAction_actionPerformed_();

    @Default(lngs = { "en" }, values = { "Window Management" })
    String GUISettings_GUISettings_object_frames();

    @Default(lngs = { "en" }, values = { "Ever got annoyed by Dialog Popups, Windows coming to front, Captcha Dialogs that steal the focus from other Windows or anything else Window or focus related? Yes? We're sorry! Please take the following options as our apology." })
    String GUISettings_GUISettings_object_frames_description();

    @Default(lngs = { "en" }, values = { "Silent Mode" })
    String GUISettings_GUISettings_sielntMode();

    @Default(lngs = { "en" }, values = { "If the Silentmode is enabled, you will not get any Popup, Dialogs, Captcharequests or other notifications unless JDownloader is your active Window." })
    String GUISettings_GUISettings_sielntMode_description();

    @Default(lngs = { "en" }, values = { "according to %s1 specifications" })
    String GUISettings_GUISettings_framestate_os_default(String string);

    @Default(lngs = { "en" }, values = { "behind all windows" })
    String GUISettings_GUISettings_framestate_back();

    @Default(lngs = { "en" }, values = { "in front of all windows" })
    String GUISettings_GUISettings_framestate_front();

    @Default(lngs = { "en" }, values = { "in front of all windows and focused" })
    String GUISettings_GUISettings_framestate_focus();

    @Default(lngs = { "en" }, values = { "Show new Dialogs..." })
    String GUISettings_GUISettings_dialog_focus();

    @Default(lngs = { "en" }, values = { "When new Links were added, then..." })
    String GUISettings_GUISettings_dialog_linkgrabber_on_new_links();

    @Default(lngs = { "en" }, values = { "do nothing" })
    String GUISettings_GUISettings_newlinks_nothing();

    @Default(lngs = { "en" }, values = { "show linkgrabber in front of all other windows" })
    String GUISettings_GUISettings_newlinks_front();

    @Default(lngs = { "en" }, values = { "show linkgrabber in front of all other windows & steal the focus" })
    String GUISettings_GUISettings_newlinks_focus();

    @Default(lngs = { "en" }, values = { "Extraction error" })
    String TaskColumn_getStringValue_extraction_error();

    @Default(lngs = { "en" }, values = { "Extraction error: CRC" })
    String TaskColumn_getStringValue_extraction_error_crc();

    @Default(lngs = { "en" }, values = { "Extraction error: Not enough Space" })
    String TaskColumn_getStringValue_extraction_error_space();

    @Default(lngs = { "en" }, values = { "Extraction error: File not found" })
    String TaskColumn_getStringValue_extraction_error_file_not_found();

    @Default(lngs = { "en" }, values = { "Extraction OK" })
    String TaskColumn_getStringValue_extraction_success();

    @Default(lngs = { "en" }, values = { "Extracting..." })
    String TaskColumn_getStringValue_extraction_extracting();

    @Default(lngs = { "en" }, values = { "A System reboot is required for changes to take effect." })
    String LookAndFeelController_onConfigValueModified_reboot_required();

    @Default(lngs = { "en" }, values = { "Click here to install pending updates!" })
    String UpdateAction_runInEDT_updates_pendings();

    @Default(lngs = { "en" }, values = { "Click here to search for new updates!" })
    String UpdateAction_runInEDT_no_updates_pendings();

    @Default(lngs = { "en" }, values = { "Find Updates" })
    String CheckForUpdatesAction_CheckForUpdatesAction();

    @Default(lngs = { "en" }, values = { "Clear filtered Links" })
    String ClearFilteredLinksAction();

    @Default(lngs = { "en" }, values = { "Do you really want to remove all filtered Links(%s1)?" })
    String ClearFilteredLinksAction_msg(int count);

    @Default(lngs = { "en" }, values = { "Quick Settings" })
    String quicksettings_SettingsMenuContainer();

    @Default(lngs = { "en" }, values = { "Open notify settings" })
    String Notify_createHeader_settings_tt();

    @Default(lngs = { "en" }, values = { "Close this notification" })
    String Notify_createHeader_close_tt();

    @Default(lngs = { "en" }, values = { "Bubble Notify" })
    String NotifierConfigPanel_getTitle();

    @Default(lngs = { "en" }, values = { "New Links added" })
    String balloon_new_links();

    @Default(lngs = { "en" }, values = { "%s2 Link(s) in %s1 Package(s)" })
    String balloon_new_links_msg(int packagcount, int childrenCount);

    @Default(lngs = { "en" }, values = { "New Package added" })
    String balloon_new_package();

    @Default(lngs = { "en" }, values = { "A new Package has been added to the Linkgrabber: %s1" })
    String balloon_new_package_msg(String name);

    @Default(lngs = { "en" }, values = { "Bubble Notify" })
    String plugins_optional_JDLightTray_ballon();

    @Default(lngs = { "en" }, values = { "Bubble Notifications are tiny messages that notify you when special events occur. " })
    String plugins_optional_JDLightTray_ballon_desc();

    @Default(lngs = { "en" }, values = { "during Linkcrawling and Onlinecheck" })
    String plugins_optional_JDLightTray_ballon_newlinks3();

    @Default(lngs = { "en" }, values = { "an Update is ready for installation" })
    String plugins_optional_JDLightTray_ballon_updates2();

    @Default(lngs = { "en" }, values = { "JDownloader Updates available!" })
    String balloon_updates();

    @Default(lngs = { "en" }, values = { "New Updates for JDownloader are available.\r\nClick here to start Installation!" })
    String balloon_updates_msg();

    @Default(lngs = { "en" }, values = { "A Reconnect process starts now.\r\nA new IP is required to continue downloads." })
    String balloon_reconnect_start_msg();

    @Default(lngs = { "en" }, values = { "Reconnect failed!\r\nYour IP is %s1" })
    String balloon_reconnect_end_msg_failed(IP ip);

    @Default(lngs = { "en" }, values = { "Reconnect successful!\r\nYour new IP is %s1" })
    String balloon_reconnect_end_msg(IP ip);

    @Default(lngs = { "en" }, values = { "during a Reconnect" })
    String plugins_optional_JDLightTray_ballon_reconnectstart3();

    @Default(lngs = { "en" }, values = { "JDownloader Reconnect" })
    String balloon_reconnect();

    @Default(lngs = { "en" }, values = { "Balloon Notification is disabled!" })
    String enabled_os_ballons();

    @Default(lngs = { "en" }, values = { "JDownloader would like to show you tiny notify messages about important events, but your Windows System is set up to hide these messages.\r\nClick [Yes] if you want JDownloader to correct this and show these balloon notifications.\r\nIt may be required to reboot your System for the changes to take effect." })
    String enabled_os_ballons_msg();

    @Default(lngs = { "en" }, values = { "Bubble Settings" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_settings_();

    @Default(lngs = { "en" }, values = { "Show Bubbles while Silent Mode is active" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_silent_();

    @Default(lngs = { "en" }, values = { "Captcha Input required: %s1" })
    String CaptchaNotify_CaptchaNotify_title_(String host);

    @Default(lngs = { "en" }, values = { "Click here to solve the Captcha" })
    String CaptchaNotify_CaptchaNotifyPanel_text();

    @Default(lngs = { "en" }, values = { "No Solution for this Captcha Challenge found" })
    String CaptchaNotify_update();

    @Default(lngs = { "en" }, values = { "Pending. This Solver is still running" })
    String CaptchaNotify_pending();

    @Default(lngs = { "en" }, values = { "a Captcha Dialog is waiting in the Background" })
    String plugins_optional_JDLightTray_ballon_captcha2();

    @Default(lngs = { "en" }, values = { "Time to Live (in ms)" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_timeout();

    @Default(lngs = { "en" }, values = { "Animation duration (Fade in & Out time in ms)" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_fadetime();

    @Default(lngs = { "en" }, values = { "Reset selected link(s)?\r\nThis would reset %s1 Link(s) & delete %s3 file(s)(%s2) from harddisk." })
    String gui_downloadlist_reset2(int totalCount, String formatBytes, int localFileCount);

    @Default(lngs = { "en" }, values = { "Linkgrabber" })
    String GeneralSettingsConfigPanel_GeneralSettingsConfigPanel_linkgrabber();

    @Default(lngs = { "en" }, values = { "Group single files in a 'various package'" })
    String GeneralSettingsConfigPanel_GeneralSettingsConfigPanel_various_package();

    @Default(lngs = { "en" }, values = { "Captchas" })
    String AntiCaptchaConfigPanel_getTitle();

    @Default(lngs = { "en" }, values = { "Plugins" })
    String PluginSettings_getTitle();

    @Default(lngs = { "en" }, values = { "Many services ask you to enter a so called 'Captcha'. Usually, a captcha is a tiny image that contains a few letters. You have to type these letters to proove that you are human. JDownloader will try to solve these captchas without asking you. However, there are captchas that are too hard to read for JDownloader - thus JD will ask you." })
    String AntiCaptchaConfigPanel_onShow_description();

    @Default(lngs = { "en" }, values = { "Captcha Solver Services" })
    String AntiCaptchaConfigPanel_AntiCaptchaConfigPanel_solver();

    @Default(lngs = { "en" }, values = { "Play Notify Sound for Captchas" })
    String AntiCaptchaConfigPanel_AntiCaptchaConfigPanel_sounds();

    @Default(lngs = { "en" }, values = { "Download Captchas auto close after timeout" })
    String AntiCaptchaConfigPanel_AntiCaptchaConfigPanel_countdown_download();

    @Default(lngs = { "en" }, values = { "Some captchas are too hard for JDownloader to auto recognize. There are a few Captcha Solver Services that help you to get these captchas solved anyways..." })
    String AntiCaptchaConfigPanel_onShow_description_solver();

    @Default(lngs = { "en" }, values = { "Solver: " })
    String CESSettingsPanel_CESSettingsPanel_choose_();

    @Default(lngs = { "en" }, values = { "Captcha Exchange Service: %s1" })
    String CESSettingsPanel_runInEDT_header(String displayName);

    @Default(lngs = { "en" }, values = { "The my.JDownloader.org Service sends all captcha requests to your mobile or any other internet device. Use this service if you want to solve captchas remotely." })
    String MyJDownloaderService_createPanel_description_();

    @Default(lngs = { "en" }, values = { "Earn 'credits' by solving captchas for others. In return, others solve your captchas while you are not in front of your computer. Check out the service's website for more details." })
    String AntiCaptchaConfigPanel_onShow_description_ces();

    @Default(lngs = { "en" }, values = { "Visit the Website" })
    String lit_open_website();

    @Default(lngs = { "en" }, values = { "Enter Api Key" })
    String NinekwService_createPanel_apikey();

    @Default(lngs = { "en" }, values = { "Enter your 9kw.eu API Key below. If you do not have an account yet, click the Link above and register for an account. It's free!" })
    String NinekwService_createPanel_logins_();

    @Default(lngs = { "en" }, values = { "Enable 9kw.eu Service" })
    String NinekwService_createPanel_enabled();

    @Default(lngs = { "en" }, values = { "Blacklist" })
    String NinekwService_createPanel_blacklist();

    @Default(lngs = { "en" }, values = { "Whitelist" })
    String NinekwService_createPanel_whitelist();

    @Default(lngs = { "en" }, values = { "With Mouse" })
    String NinekwService_createPanel_mouse();

    @Default(lngs = { "en" }, values = { "Feedback" })
    String NinekwService_createPanel_feedback();

    @Default(lngs = { "en" }, values = { "Https" })
    String NinekwService_createPanel_https();

    @Default(lngs = { "en" }, values = { "Confirm (Cost +6)" })
    String NinekwService_createPanel_confirm();

    @Default(lngs = { "en" }, values = { "Mouse Confirm (Cost +6)" })
    String NinekwService_createPanel_mouseconfirm();

    @Default(lngs = { "en" }, values = { "Selfsolve" })
    String NinekwService_createPanel_selfsolve();

    @Default(lngs = { "en" }, values = { "Prio 1-10 (Cost +1-10)" })
    String NinekwService_createPanel_prio();

    @Default(lngs = { "en" }, values = { "Captcha per hour" })
    String NinekwService_createPanel_hour();

    @Default(lngs = { "en" }, values = { "Enter your Captcha Brotherhood Logins below. If you do not have an account yet, click the Link above and register for an account. It's free!" })
    String captchabrotherhoodService_createPanel_logins_();

    @Default(lngs = { "en" }, values = { "Username" })
    String captchabrotherhoodService_createPanel_username();

    @Default(lngs = { "en" }, values = { "Password" })
    String captchabrotherhoodService_createPanel_password();

    @Default(lngs = { "en" }, values = { "Finding Links..." })
    String LinkCrawlerBubble_update_header();

    @Default(lngs = { "en" }, values = { "Parse Clipboard" })
    String LinkCrawlerBubble_update_header_from_Clipboard();

    @Default(lngs = { "en" }, values = { "Running for %s2...\r\nFound Links: %s1" })
    String LinkCrawlerBubble_update_running_linkcrawler(int crawledLinksFoundCounter, String time);

    @Default(lngs = { "en" }, values = { "Finished in %s2...\r\nFound Links: %s1" })
    String LinkCrawlerBubble_update_stopped_linkcrawler(int crawledLinksFoundCounter, String formatMilliSeconds);

    @Default(lngs = { "en" }, values = { "Parse Clipboard: %s1" })
    String LinkCrawlerBubble_update_header_from_Clipboard_url(String txt);

    @Default(lngs = { "en" }, values = { "Pending Downloads" })
    String downloadview_todo();

    @Default(lngs = { "en" }, values = { "a dedicated plugin (no direct http)" })
    String ConditionDialog_layoutDialogContent_directhttp();

    @Default(lngs = { "en" }, values = { "Link has no dedicated Plugin(direct http)" })
    String FilterRule_toString_directhttp();

    @Default(lngs = { "en" }, values = { "Link has a dedicated Plugin" })
    String FilterRule_toString_directhttp_not();

    @Default(lngs = { "en" }, values = { "Start all Downloads" })
    String ConfirmAllContextmenuAction_context_add_and_start();

    @Default(lngs = { "en" }, values = { "Add all to Download List" })
    String ConfirmAllContextmenuAction_context_add();

    @Default(lngs = { "en" }, values = { "Stop Downloads, but finish running ones" })
    String StopDownloadsAction_createTooltip();

    @Default(lngs = { "en" }, values = { "%s1 - %s2/s" })
    String JDGui_updateTitle_speed_(String title, String speed);

    @Default(lngs = { "en" }, values = { "Overview" })
    String AccountManager_AccountManager_accounts_();

    @Default(lngs = { "en" }, values = { "Account Usage Rules" })
    String AccountManager_AccountManager_hosterorder_();

    @Default(lngs = { "en" }, values = { "Default Account Priority" })
    String HosterOrderPanel_getTextForValue_default_();

    @Default(lngs = { "en" }, values = { "%s1's Account Priority" })
    String HosterOrderPanel_getTextForValue(String tld);

    @Default(lngs = { "en" }, values = { "Account Group" })
    String FileColumn_getStringValue_accountgroup_();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String HosterRuleTableModel_initColumns_hoster_();

    @Default(lngs = { "en" }, values = { "Use Hoster Rules if you have more than one Premium Account and want to specify in which order these accounts should be used." })
    String HosterOrderPanel_HosterOrderPanel_description_();

    @Default(lngs = { "en" }, values = { "Enabled Captchabrotherhood's Service" })
    String captchabrotherhoodService_createPanel_enabled();

    @Default(lngs = { "en" }, values = { "Add, remove or modify premium accounts" })
    String AccountManager_AccountManager_accounts_tt();

    @Default(lngs = { "en" }, values = { "Set up rules to control the account order, in case you have more than one account for an hoster" })
    String AccountManager_AccountManager_hosterorder_tt();

    @Default(lngs = { "en" }, values = { "Add a new Rule" })
    String NewRuleAction_getTooltipText_tt_();

    @Default(lngs = { "en" }, values = { "Please enter a Domain" })
    String NewRuleAction_actionPerformed_choose_hoster_();

    @Default(lngs = { "en" }, values = { "Please choose a Hoster to create a rule for.\r\nIf the desired Hoster is missing, you probably do not have an account for this hoster.\r\nMake sure to add an Account first in the Account Overview." })
    String NewRuleAction_actionPerformed_choose_hoster_message();

    @Default(lngs = { "en" }, values = { "Edit" })
    String HosterRuleTableModel_initColumns_edit_();

    @Default(lngs = { "en" }, values = { "Remove %s1 rule(s)?" })
    String accountUsageRule_remove_action_title(int size);

    @Default(lngs = { "en" }, values = { "Do you really want to remove these rule(s)?\r\n%s1" })
    String accountUsageRule_remove_action_msg(String string);

    @Default(lngs = { "en" }, values = { "Account Usage Rule for %s1" })
    String EditHosterRuleDialog_EditHosterRuleDialog_title_(String hoster);

    @Default(lngs = { "en" }, values = { "Add Group" })
    String AddGroupAction_AddGroupAction();

    @Default(lngs = { "en" }, values = { "Distribution Rule" })
    String GroupRuleColumn_GroupRuleColumn_distrubutionrule_();

    @Default(lngs = { "en" }, values = { "Randomly use an account of this group" })
    String Rules_random();

    @Default(lngs = { "en" }, values = { "First comes first - Use the given order" })
    String Rules_order();

    @Default(lngs = { "en" }, values = { "There is nothing to delete here!" })
    String GenericDeleteSelectedToolbarAction_actionPerformed_nothing_to_delete_();

    @Default(lngs = { "en" }, values = { "Clear Downloadlist" })
    String GenericDeleteSelectedToolbarAction_updateName_object_all();

    @Default(lngs = { "en" }, values = { "Delete Links: " })
    String GenericDeleteSelectedToolbarAction_updateName_object();

    @Default(lngs = { "en" }, values = { "Below, all premium accounts and the 'free download' option for %s1 are listed. You can set up a custom order here. JDownloader will try to use the account(s) in the first group for downloading. If all Accounts in the first group fail, it will try the second group, and so on. If there are several accounts in one group, the group's 'Distribution Rule' will be used to select the next account." })
    String EditHosterRuleDialog_layoutDialogContent_description_(String hoster);

    @Default(lngs = { "en" }, values = { "Remove group" })
    String DeleteGroupAction_DeleteGroupAction();

    @Default(lngs = { "en" }, values = { "Single Hoster Accounts" })
    String HosterRuleController_validateRule_single_hoster_account();

    @Default(lngs = { "en" }, values = { "Multi Hoster Accounts" })
    String HosterRuleController_validateRule_multi_hoster_account();

    @Default(lngs = { "en" }, values = { "Download without any Account (Free Mode)" })
    String HosterRuleController_validateRule_free();

    @Default(lngs = { "en" }, values = { "Free Download (No Account)" })
    String PackageColumn_getStringValue_freedownload_();

    @Default(lngs = { "en" }, values = { "Reset the current plugin's settings to default." })
    String PluginSettingsPanel_PluginSettingsPanel_reset();

    @Default(lngs = { "en" }, values = { "The %s1 Plugin Settings have been resetted to default values!" })
    String PluginSettingsPanel_actionPerformed_reset_done(String domain);

    @Default(lngs = { "en" }, values = { "%s1 (Multi Hoster)" })
    String AccountTooltip_AccountTooltip_multi(String tld);

    @Default(lngs = { "en" }, values = { "With these account(s), you can download from:" })
    String AccountTooltip_AccountTooltip_supported_hosters();

    @Default(lngs = { "en" }, values = { "Are you sure that you want to reset all plugin settings for the %s1 plugin?" })
    String PluginSettingsPanel_are_you_sure(String displayName);

    @Default(lngs = { "en" }, values = { "Credit Balance:" })
    String ServicePanel9kwTooltip_runInEDT_credits_();

    @Default(lngs = { "en" }, values = { "Error while loading 9kw.eu Account Information" })
    String ServicePanel9kwTooltip_runInEDT_error();

    @Default(lngs = { "en" }, values = { "Solved Captchas:" })
    String ServicePanel9kwTooltip_runInEDT_solved();

    @Default(lngs = { "en" }, values = { "Answered Captchas:" })
    String ServicePanel9kwTooltip_runInEDT_answered();

    @Default(lngs = { "en" }, values = { "Account Error: %s1" })
    String ServicePanel9kwTooltip_runInEDT_error2(String error);

    @Default(lngs = { "en" }, values = { "Delete" })
    String DeleteMenuContainer_DeleteMenuContainer_delete_();

    @Default(lngs = { "en" }, values = { "Download list Bottombar Menumanager" })
    String BottomBarMenuManager_getName();

    @Default(lngs = { "en" }, values = { "Add Links" })
    String AddLinksContainer_AddLinksContainer();

    @Default(lngs = { "en" }, values = { "Filter & Searchbar" })
    String FilterMenuItem_FilterMenuItem();

    @Default(lngs = { "en" }, values = { "Table Quickfilter" })
    String QuickFilterMenuItem_QuickFilterMenuItem();

    @Default(lngs = { "en" }, values = { "Download Tab Bottom Panel" })
    String gui_config_menumanager_downloadBottom();

    @Default(lngs = { "en" }, values = { "Apply" })
    String lit_apply();

    @Default(lngs = { "en" }, values = { "Customize this Bottom Panel" })
    String BottomBarMenuManagerAction_BottomBarMenuManagerAction();

    @Default(lngs = { "en" }, values = { "Horizontal expanding empty Box" })
    String HorizontalBoxItem_HorizontalBoxItem();

    @Default(lngs = { "en" }, values = { "All visible" })
    String lit_all_visible();

    @Default(lngs = { "en" }, values = { "Are you sure that you want to do this:\r\n%s1" })
    String GenericDeleteFromDownloadlistAction_actionPerformed_ask_(String taskname);

    @Default(lngs = { "en" }, values = { "Clear Linkgrabber List" })
    String GenericDeleteFromLinkgrabberAction_createName_updateName_object_all();

    @Default(lngs = { "en" }, values = { "Are you sure that you want to do this: %s1\r\nLinks affected: %s2" })
    String GenericDeleteFromLinkgrabberContextAction_actionPerformed_ask_(String createName, int linkcount);

    @Default(lngs = { "en" }, values = { "Auto Confirm Button" })
    String AutoConfirmMenuLink_getName();

    @Default(lngs = { "en" }, values = { "Sidebar Seperator: |" })
    String LeftRightDividerItem_LeftRightDividerItem();

    @Default(lngs = { "en" }, values = { "Linkgrabber Bottom Bar" })
    String gui_config_menumanager_linkgrabberBottom();

    @Default(lngs = { "en" }, values = { "Overview Panel visible" })
    String DownloadsOverviewPanelToggleAction_DownloadsOverviewPanelToggleAction();

    @Default(lngs = { "en" }, values = { "Overview Panel visible" })
    String LinkgrabberOverviewPanelToggleAction_LinkgrabberOverviewPanelToggleAction();

    @Default(lngs = { "en" }, values = { "Sidebar visible" })
    String LinkgrabberSidebarToggleAction_LinkgrabberSidebarToggleAction();

    @Default(lngs = { "en" }, values = { "Add & Analyse Links from your Clipboard" })
    String PasteLinksAction_PasteLinksAction();

    @Default(lngs = { "en" }, values = { "Add & DeepAnalyse Links from your Clipboard" })
    String PasteLinksAction_PasteLinksAction_deep();

    @Default(lngs = { "en" }, values = { "Filename:" })
    String IfFileExistsDialog_layoutDialogContent_filename();

    @Default(lngs = { "en" }, values = { "Package:" })
    String IfFileExistsDialog_layoutDialogContent_package();

    @Default(lngs = { "en" }, values = { "Hoster:" })
    String IfFileExistsDialog_layoutDialogContent_hoster();

    @Default(lngs = { "en" }, values = { "New File's size:" })
    String IfFileExistsDialog_layoutDialogContent_filesize2();

    @Default(lngs = { "en" }, values = { "Archivename:" })
    String ExtractionListenerList_layoutDialogContent_archivename();

    @Default(lngs = { "en" }, values = { "First File:" })
    String ExtractionListenerList_layoutDialogContent_filename();

    @Default(lngs = { "en" }, values = { "Password:" })
    String ExtractionListenerList_layoutDialogContent_password();

    @Default(lngs = { "en" }, values = { "Reset to Default?" })
    String MenuManagerDialog_actionPerformed_title();

    @Default(lngs = { "en" }, values = { "Package or Link Properties" })
    String LinkgrabberPropertiesToggleAction_LinkgrabberPropertiesToggleAction();

    @Default(lngs = { "en" }, values = { "Package Properties: %s1" })
    String LinkgrabberPropertiesHeader_update_package(String name);

    @Default(lngs = { "en" }, values = { "File Properties: %s1" })
    String LinkgrabberPropertiesHeader_update_link(String name);

    @Default(lngs = { "en" }, values = { "Save to:" })
    String propertiespanel_downloadpath();

    @Default(lngs = { "en" }, values = { "Package name:" })
    String propertiespanel_packagename();

    @Default(lngs = { "en" }, values = { "Comment:" })
    String propertiespanel_comment();

    @Default(lngs = { "en" }, values = { "Archive Password:" })
    String propertiespanel_archivepassword();

    @Default(lngs = { "en" }, values = { "Download Password:" })
    String propertiespanel_downloadpassword();

    @Default(lngs = { "en" }, values = { "Panel has been hidden" })
    String DownloadsPanel_onCloseAction();

    @Default(lngs = { "en" }, values = { "you clicked the close button to hide this panel.\r\nIf you ever feel like getting it back, use the settings button in the bar at the bottom right!" })
    String DownloadsPanel_onCloseAction_help();

    @Default(lngs = { "en" }, values = { "you clicked the close button to hide the properties panel.\r\nIf you ever feel like getting it back, use the settings button in the bar at the bottom right\r\nor rightclick on a link/package in the table to open the context menu.!" })
    String Linkgrabber_properties_onCloseAction_help();

    @Default(lngs = { "en" }, values = { "You activated the properties panel!" })
    String LinkGrabberPanel_setPropertiesPanelVisible();

    @Default(lngs = { "en" }, values = { "...the properties panel is not visible right now, because there is no package or Downloadlink selected. \r\nTo see the properties panel, add Downloads to the Linkgrabber an click on a table row." })
    String LinkGrabberPanel_setPropertiesPanelVisible_help();

    @Default(lngs = { "en" }, values = { "Auto Extract Disabled" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextractdisabled();

    @Default(lngs = { "en" }, values = { "Auto Extract Enabled" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextractenabled();

    @Default(lngs = { "en" }, values = { "Global Setting(Auto Extract Enabled)" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_true();

    @Default(lngs = { "en" }, values = { "Global Setting(Auto Extract disable)" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_false();

    @Default(lngs = { "en" }, values = { "Auto Extract Disabled" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextractdisabled_closed();

    @Default(lngs = { "en" }, values = { "Auto Extract Enabled" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextractenabled_closed();

    @Default(lngs = { "en" }, values = { "Auto Extract Enabled*" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_true_closed();

    @Default(lngs = { "en" }, values = { "Auto Extract Disable*" })
    String PackagePropertiesPanel_getListCellRendererComponent_autoextract_default_false_closed();

    @Default(lngs = { "en" }, values = { "Mixed Priority" })
    String PackagePropertiesPanel_getLabel_mixed_priority();

    @Default(lngs = { "en" }, values = { "File name:" })
    String propertiespanel_filename();

    @Default(lngs = { "en" }, values = { "The File name" })
    String AddLinksDialog_layoutDialogContent_filename_tt();

    @Default(lngs = { "en" }, values = { "Properties" })
    String PropertiesAction_PropertiesAction();

    @Default(lngs = { "en" }, values = { "Speedmeter visible" })
    String SpeedlimitToggleAction_SpeedlimitToggleAction();

    @Default(lngs = { "en" }, values = { "Open Toolbar manager" })
    String MenuManagerMainToolbarAction_MenuManagerMainToolbarAction();

    @Default(lngs = { "en" }, values = { "Pause" })
    String download_paused();

    @Default(lngs = { "en" }, values = { "All downloads have been paused." })
    String download_paused_msg();

    @Default(lngs = { "en" }, values = { "Stop" })
    String download_stopped();

    @Default(lngs = { "en" }, values = { "All downloads stopped or finished." })
    String download_stopped_msg();

    @Default(lngs = { "en" }, values = { "Start" })
    String download_start();

    @Default(lngs = { "en" }, values = { "Go, go, go! Downloads are running now!" })
    String download_start_msg();

    @Default(lngs = { "en" }, values = { "A Download started!" })
    String DownloadStartNotify_DownloadStartNotify();

    @Default(lngs = { "en" }, values = { "Filename" })
    String lit_filename();

    @Default(lngs = { "en" }, values = { "Hoster" })
    String lit_hoster();

    @Default(lngs = { "en" }, values = { "Account" })
    String lit_account();

    @Default(lngs = { "en" }, values = { "Proxy" })
    String lit_proxy();

    @Default(lngs = { "en" }, values = { "Save to" })
    String lit_save_to();

    @Default(lngs = { "en" }, values = { "A Download stopped!" })
    String DownloadStoppedNotify();

    @Default(lngs = { "en" }, values = { "Status" })
    String lit_status();

    @Default(lngs = { "en" }, values = { "a Downloadlink started or stopped" })
    String plugins_optional_JDLightTray_ballon_startstopdownloads2();

    @Default(lngs = { "en" }, values = { "the Download started, stopped or has been paused " })
    String plugins_optional_JDLightTray_ballon_startpausestop2();

    @Default(lngs = { "en" }, values = { "Sort Downloadorder on '%s1'" })
    String SortPackagesDownloadOrdnerOnColumn(String name);

    @Default(lngs = { "en" }, values = { "Archive Part" })
    String LinkGrabberTableModel_partcolumn();

    @Default(lngs = { "en" }, values = { "Download Password:" })
    String propertiespanel_passwod();

    @Default(lngs = { "en" }, values = { "Enter the password required to download the file..." })
    String AddLinksDialog_layoutDialogContent_password_tt();

    @Default(lngs = { "en" }, values = { "MD5/SHA1:" })
    String propertiespanel_checksum();

    @Default(lngs = { "en" }, values = { "Enter the MD5 or SHA1 Checksum..." })
    String AddLinksDialog_layoutDialogContent_checksum_tt();

    @Default(lngs = { "en" }, values = { "Link origin" })
    String FilterRuleDialog_layoutDialogContent_lbl_crawlersource();

    @Default(lngs = { "en" }, values = { "No Selection" })
    String PseudoMultiCombo_nothing();

    @Default(lngs = { "en" }, values = { "Custom Type" })
    String ConditionDialog_getLabel_customtype_();

    @Default(lngs = { "en" }, values = { "Unknown" })
    String OriginFilter_toString_nothing();

    @Default(lngs = { "en" }, values = { "Link Source is: %s1" })
    String OriginFilter_toString(String string);

    @Default(lngs = { "en" }, values = { "Link Source is not: %s1" })
    String OriginFilter_toString_isNot(String string);

    @Default(lngs = { "en" }, values = { "Auto Forced Download Start" })
    String PackagizerFilterRuleDialog_layoutDialogContent_force();

    @Default(lngs = { "en" }, values = { "Open Links in Browser" })
    String OpenInBrowserAction_actionPerformed_open_in_browser__multi();

    @Default(lngs = { "en" }, values = { "You choose to open %s1 Links in your browser." })
    String OpenInBrowserAction_actionPerformed_open_in_browser__multi_msg(int size);

    @Default(lngs = { "en" }, values = { "Unknown" })
    String SizeColumn_getSizeString_zero();

    @Default(lngs = { "en" }, values = { "Captchas solved by the Captchabrotherhood Captcha Exchange System" })
    String CaptchaBrotherhoodService_getDescription_tt_();

    @Default(lngs = { "en" }, values = { "Captcha solved by My.JDownloader Remote Applications (Webinterface,Mobile Apps,...)" })
    String MyJDownloaderService_getDescription_tt_();

    @Default(lngs = { "en" }, values = { "Captchas solved by the 9kw.eu Captcha Exchange System" })
    String NinekwService_getDescription_tt_();

    @Default(lngs = { "en" }, values = { "Always" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_always();

    @Default(lngs = { "en" }, values = { "Only if JDownloader is not the active application" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_jdnotactive();

    @Default(lngs = { "en" }, values = { "Only if JDownloader is minimized to tray or taskbar" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_trayortask();

    @Default(lngs = { "en" }, values = { "Only if JDownloader is minimized to taskbar" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_taskbar();

    @Default(lngs = { "en" }, values = { "Only if JDownloader is minimized to tray" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_tray();

    @Default(lngs = { "en" }, values = { "Never" })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_never();

    @Default(lngs = { "en" }, values = { "Show Bubbles if..." })
    String BubbleNotifyConfigPanel_BubbleNotifyConfigPanel_enabledstate();

    @Default(lngs = { "en" }, values = { "and" })
    String lit_and();

    @Default(lngs = { "en" }, values = { "or" })
    String lit_or();

    @Default(lngs = { "en" }, values = { "Memory Problem detected!" })
    String MEMORY_RESTART_TITLE();

    @Default(lngs = { "en" }, values = { "I seems that there is a memory Problem. A Restart of JDownloader is required to fix this problem.\r\nIf even 2 restarts do not fix it, please visit our support chat." })
    String MEMORY_RESTART_MSG();

    @Default(lngs = { "en" }, values = { "Restart" })
    String lit_restart();

    @Default(lngs = { "en" }, values = { "Support Chat" })
    String memory_chat();

    @Default(lngs = { "en" }, values = { "Result:" })
    String ReconnectBubbleContent_onResult_result();

    @Default(lngs = { "en" }, values = { "Found Link(s)" })
    String LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundlink();

    @Default(lngs = { "en" }, values = { "Found Package(s)" })
    String LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundpackages();

    @Default(lngs = { "en" }, values = { "Offline" })
    String LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundoffline();

    @Default(lngs = { "en" }, values = { "Status" })
    String LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_status();

    @Default(lngs = { "en" }, values = { "Crawling..." })
    String LinkCrawlerBubbleContent_update_runnning();

    @Default(lngs = { "en" }, values = { "Done!" })
    String LinkCrawlerBubbleContent_update_finished();

    @Default(lngs = { "en" }, values = { "Onlinecheck..." })
    String LinkCrawlerBubbleContent_update_online();

    @Default(lngs = { "en" }, values = { "Online" })
    String LinkCrawlerBubbleContent_LinkCrawlerBubbleContent_foundonline();

    @Default(lngs = { "en" }, values = { "Click to open the Panel's Settings" })
    String AbstractPanelHeader_AbstractPanelHeader_settings_tt();

    @Default(lngs = { "en" }, values = { "Show Download folder" })
    String LinkgrabberPropertiesHeader_saveto();

    @Default(lngs = { "en" }, values = { "Show Filename" })
    String LinkgrabberPropertiesHeader_filename();

    @Default(lngs = { "en" }, values = { "Show Packagename" })
    String LinkgrabberPropertiesHeader_packagename();

    @Default(lngs = { "en" }, values = { "Show Download Password" })
    String LinkgrabberPropertiesHeader_downloadpassword();

    @Default(lngs = { "en" }, values = { "Show Checksum (MD5/SHA1)" })
    String LinkgrabberPropertiesHeader_checksum();

    @Default(lngs = { "en" }, values = { "Show Comment & Priority" })
    String LinkgrabberPropertiesHeader_comment_and_priority();

    @Default(lngs = { "en" }, values = { "Show Archive Information" })
    String LinkgrabberPropertiesHeader_archiveline();

    @Default(lngs = { "en" }, values = { "Show Download from" })
    String LinkgrabberPropertiesHeader_downloadfrom();

    @Default(lngs = { "en" }, values = { "Download from" })
    String propertiespanel_downloadfrom();

    @Default(lngs = { "en" }, values = { "The address behind the selected entry" })
    String AddLinksDialog_layoutDialogContent_downloadfrom_tt();

    @Default(lngs = { "en" }, values = { "Exiting File's size:" + "" })
    String IfFileExistsDialog_layoutDialogContent_filesize_existing();

    @Default(lngs = { "en" }, values = { "Enter predefined package name matcher... (Use * as Wildcard)" })
    String FilterRuleDialog_layoutDialogContent_ht_Package();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String FilterRuleDialog_layoutDialogContent_lbl_Package();

    @Default(lngs = { "en" }, values = { "Package Name" })
    String ConditionDialog_getPopupMenu_Package_();

    @Default(lngs = { "en" }, values = { "Package Name Wildcard(*) #%s1" })
    String PackagizerFilterRuleDialog_createVariablesMenu_package(int i);

}