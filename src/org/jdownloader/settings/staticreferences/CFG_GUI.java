package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ListHandler;
import org.appwork.storage.config.handler.LongKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.storage.config.handler.StringListHandler;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class CFG_GUI {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(GraphicalUserInterfaceSettings.class);

    }

    // Static Mappings for interface org.jdownloader.settings.GraphicalUserInterfaceSettings
    public static final GraphicalUserInterfaceSettings                 CFG                                                     = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                                      = (StorageHandler<GraphicalUserInterfaceSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Blacklist(regex) of processes to ignore Clipboard monitoring
     **/
    public static final StringListHandler                              CLIPBOARD_PROCESS_BLACKLIST                             = SH.getKeyHandler("ClipboardProcessBlacklist", StringListHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE         = SH.getKeyHandler("LinkPropertiesPanelDownloadPasswordVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_HOSTER_COUNT_VISIBLE         = SH.getKeyHandler("OverviewPanelLinkgrabberHosterCountVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_LINKS_FINISHED_COUNT_VISIBLE    = SH.getKeyHandler("OverviewPanelDownloadLinksFinishedCountVisible", BooleanKeyHandler.class);

    /**
     * Enable/disable Clipboard monitoring
     **/
    public static final BooleanKeyHandler                              CLIPBOARD_MONITORED                                     = SH.getKeyHandler("ClipboardMonitored", BooleanKeyHandler.class);

    /**
     * Enable/Disable the Linkgrabber properties panel
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE                = SH.getKeyHandler("LinkgrabberTabPropertiesPanelVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE              = SH.getKeyHandler("DownloadsPropertiesPanelSaveToVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 DONATE_BUTTON_STATE                                     = SH.getKeyHandler("DonateButtonState", EnumKeyHandler.class);

    /**
     * If false, Most of the Tooltips will be disabled
     **/
    public static final BooleanKeyHandler                              TOOLTIP_ENABLED                                         = SH.getKeyHandler("TooltipEnabled", BooleanKeyHandler.class);

    /**
     * Presentation mode peforms tasks like: account username obstruction throughout GUI
     **/
    public static final BooleanKeyHandler                              PRESENTATION_MODE_ENABLED                               = SH.getKeyHandler("PresentationModeEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_PACKAGE_COUNT_VISIBLE           = SH.getKeyHandler("OverviewPanelDownloadPackageCountVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_COMMENT_VISIBLE                   = SH.getKeyHandler("LinkPropertiesPanelCommentVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE              = SH.getKeyHandler("DownloadsPropertiesPanelCommentVisible", BooleanKeyHandler.class);

    /**
     * Enable/Disable the Linkgrabber Overview panel
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_TAB_OVERVIEW_VISIBLE                        = SH.getKeyHandler("LinkgrabberTabOverviewVisible", BooleanKeyHandler.class);

    public static final StringKeyHandler                               DATE_TIME_FORMAT_DOWNLOAD_LIST_ADDED_DATE_COLUMN        = SH.getKeyHandler("DateTimeFormatDownloadListAddedDateColumn", StringKeyHandler.class);

    /**
     * Enable/Disable the DownloadPanel Overview panel
     **/
    public static final BooleanKeyHandler                              DOWNLOAD_TAB_OVERVIEW_VISIBLE                           = SH.getKeyHandler("DownloadTabOverviewVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              PREMIUM_DISABLED_WARNING_FLASH_ENABLED                  = SH.getKeyHandler("PremiumDisabledWarningFlashEnabled", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                              SPEED_METER_TIME_FRAME                                  = SH.getKeyHandler("SpeedMeterTimeFrame", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                              TASK_BAR_FLASH_ENABLED                                  = SH.getKeyHandler("TaskBarFlashEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              PASSWORD_PROTECTION_ENABLED                             = SH.getKeyHandler("PasswordProtectionEnabled", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                              DIALOG_DEFAULT_TIMEOUT_IN_MS                            = SH.getKeyHandler("DialogDefaultTimeoutInMS", IntegerKeyHandler.class);

    public static final EnumKeyHandler                                 PACKAGE_DOUBLE_CLICK_ACTION                             = SH.getKeyHandler("PackageDoubleClickAction", EnumKeyHandler.class);

    /**
     * If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_ETACOLUMN_ENABLED                         = SH.getKeyHandler("PremiumAlertETAColumnEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE                   = SH.getKeyHandler("LinkPropertiesPanelSaveToVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_TOTAL_INFO_VISIBLE                       = SH.getKeyHandler("OverviewPanelTotalInfoVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              CONFIG_VIEW_VISIBLE                                     = SH.getKeyHandler("ConfigViewVisible", BooleanKeyHandler.class);

    /**
     * If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_TASK_COLUMN_ENABLED                       = SH.getKeyHandler("PremiumAlertTaskColumnEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              PRIORITY_COLUMN_AUTO_SHOW_ENABLED                       = SH.getKeyHandler("PriorityColumnAutoShowEnabled", BooleanKeyHandler.class);

    /**
     * Hide the package in case it only contains one child
     **/
    public static final BooleanKeyHandler                              HIDE_SINGLE_CHILD_PACKAGES                              = SH.getKeyHandler("HideSingleChildPackages", BooleanKeyHandler.class);

    /**
     * Choose what should happen when you click on the [Start Downloads] Button when you are in the Linkgrabber Tab
     **/
    public static final EnumKeyHandler                                 START_BUTTON_ACTION_IN_LINKGRABBER_CONTEXT              = SH.getKeyHandler("StartButtonActionInLinkgrabberContext", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              CAPTCHA_DIALOG_UNIQUE_POSITION_BY_HOSTER_ENABLED        = SH.getKeyHandler("CaptchaDialogUniquePositionByHosterEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE           = SH.getKeyHandler("LinkPropertiesPanelArchivepasswordVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 SPEED_IN_WINDOW_TITLE                                   = SH.getKeyHandler("SpeedInWindowTitle", EnumKeyHandler.class);

    public static final IntegerKeyHandler                              TOOLTIP_DELAY                                           = SH.getKeyHandler("TooltipDelay", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                              AVAILABLE_COLUMN_TEXT_VISIBLE                           = SH.getKeyHandler("AvailableColumnTextVisible", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                               LAST_FRAME_STATUS                                       = SH.getKeyHandler("LastFrameStatus", ObjectKeyHandler.class);

    public static final EnumKeyHandler                                 FILE_CHOOSER_VIEW                                       = SH.getKeyHandler("FileChooserView", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_LINKS_SKIPPED_COUNT_VISIBLE     = SH.getKeyHandler("OverviewPanelDownloadLinksSkippedCountVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 SELECTED_LINKGRABBER_SEARCH_CATEGORY                    = SH.getKeyHandler("SelectedLinkgrabberSearchCategory", EnumKeyHandler.class);

    /**
     * Customize the order of the Overview Panel Entries in x and y position
     **/
    public static final ObjectKeyHandler                               OVERVIEW_POSITIONS                                      = SH.getKeyHandler("OverviewPositions", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                              CAPTCHA_DEBUG_MODE_ENABLED                              = SH.getKeyHandler("CaptchaDebugModeEnabled", BooleanKeyHandler.class);

    /**
     * Interval of the Downloads Table Refresher. Default 1000ms (once per second). Decreasing this value will cost CPU Power
     **/
    public static final LongKeyHandler                                 DOWNLOADS_TABLE_REFRESH_INTERVAL                        = SH.getKeyHandler("DownloadsTableRefreshInterval", LongKeyHandler.class);

    public static final EnumKeyHandler                                 PREMIUM_STATUS_BAR_DISPLAY                              = SH.getKeyHandler("PremiumStatusBarDisplay", EnumKeyHandler.class);

    /**
     * If Enabled, JDownloader will try to be always on top of all other windows
     **/
    public static final BooleanKeyHandler                              MAIN_WINDOW_ALWAYS_ON_TOP                               = SH.getKeyHandler("MainWindowAlwaysOnTop", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE    = SH.getKeyHandler("DownloadsPropertiesPanelDownloadPasswordVisible", BooleanKeyHandler.class);

    public static final StringKeyHandler                               ACTIVE_PLUGIN_CONFIG_PANEL                              = SH.getKeyHandler("ActivePluginConfigPanel", StringKeyHandler.class);

    public static final StringKeyHandler                               ACTIVE_MY_JDOWNLOADER_PANEL                             = SH.getKeyHandler("ActiveMyJDownloaderPanel", StringKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_CONNECTIONS_VISIBLE             = SH.getKeyHandler("OverviewPanelDownloadConnectionsVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              FILE_COUNT_IN_SIZE_COLUMN_VISIBLE                       = SH.getKeyHandler("FileCountInSizeColumnVisible", BooleanKeyHandler.class);

    /**
     * Use horizontal Scrollbars in Linkgrabber
     **/
    public static final BooleanKeyHandler                              HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED      = SH.getKeyHandler("HorizontalScrollbarsInLinkgrabberTableEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_STATUS_UNKNOWN_VISIBLE       = SH.getKeyHandler("OverviewPanelLinkgrabberStatusUnknownVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE                = SH.getKeyHandler("DownloadPanelOverviewSettingsVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_ETAVISIBLE                      = SH.getKeyHandler("OverviewPanelDownloadETAVisible", BooleanKeyHandler.class);

    /**
     * Days to keep disabled accounts displayed in PremiumBar and AccountToolTip overviews.
     **/
    public static final IntegerKeyHandler                              PREMIUM_STATUS_BAR_DISABLED_ACCOUNT_EXPIRE              = SH.getKeyHandler("PremiumStatusBarDisabledAccountExpire", IntegerKeyHandler.class);

    public static final EnumKeyHandler                                 NEW_DIALOG_FRAME_STATE                                  = SH.getKeyHandler("NewDialogFrameState", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE             = SH.getKeyHandler("LinkPropertiesPanelDownloadFromVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 WINDOWS_TASKBAR_PROGRESS_DISPLAY                        = SH.getKeyHandler("WindowsTaskbarProgressDisplay", EnumKeyHandler.class);

    public static final ListHandler<int[]>                             LINKGRABBER_LIST_SCROLL_POSITION                        = SH.getKeyHandler("LinkgrabberListScrollPosition", ListHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_RUNNING_DOWNLOADS_COUNT_VISIBLE = SH.getKeyHandler("OverviewPanelDownloadRunningDownloadsCountVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              BYPASS_ALL_RLY_DELETE_DIALOGS_ENABLED                   = SH.getKeyHandler("BypassAllRlyDeleteDialogsEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              CLIPBOARD_DISABLED_WARNING_FLASH_ENABLED                = SH.getKeyHandler("ClipboardDisabledWarningFlashEnabled", BooleanKeyHandler.class);

    /**
     * Highlight Table in Downloadview if table is filtered
     **/
    public static final BooleanKeyHandler                              FILTER_HIGHLIGHT_ENABLED                                = SH.getKeyHandler("FilterHighlightEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_FILENAME_VISIBLE             = SH.getKeyHandler("DownloadsPropertiesPanelFilenameVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LOG_VIEW_VISIBLE                                        = SH.getKeyHandler("LogViewVisible", BooleanKeyHandler.class);

    /**
     * Choose how many 'Are you sure?' warnings you want to see (Bug me not).
     **/
    public static final EnumKeyHandler                                 RLY_WARN_LEVEL                                          = SH.getKeyHandler("RlyWarnLevel", EnumKeyHandler.class);

    /**
     * Refreshrate in ms for the DownloadView
     **/
    public static final LongKeyHandler                                 DOWNLOAD_VIEW_REFRESH                                   = SH.getKeyHandler("DownloadViewRefresh", LongKeyHandler.class);

    /**
     * If disabled, The Hostercolumn will show gray disabled icons if the link is disabled
     **/
    public static final BooleanKeyHandler                              COLORED_ICONS_FOR_DISABLED_HOSTER_COLUMN_ENABLED        = SH.getKeyHandler("ColoredIconsForDisabledHosterColumnEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_TOTAL_BYTES_VISIBLE             = SH.getKeyHandler("OverviewPanelDownloadTotalBytesVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 NEW_LINKS_ACTION                                        = SH.getKeyHandler("NewLinksAction", EnumKeyHandler.class);

    /**
     * If disabled, the captcha has no border painted, and the dialog looks like in jd09
     **/
    public static final BooleanKeyHandler                              CAPTCHA_DIALOG_BORDER_AROUND_IMAGE_ENABLED              = SH.getKeyHandler("CaptchaDialogBorderAroundImageEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 MAX_SIZE_UNIT                                           = SH.getKeyHandler("MaxSizeUnit", EnumKeyHandler.class);

    /**
     * Packages get a different background color if enabled
     **/
    public static final BooleanKeyHandler                              PACKAGES_BACKGROUND_HIGHLIGHT_ENABLED                   = SH.getKeyHandler("PackagesBackgroundHighlightEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler                               DATE_TIME_FORMAT_DOWNLOAD_LIST_FINISHED_DATE_COLUMN     = SH.getKeyHandler("DateTimeFormatDownloadListFinishedDateColumn", StringKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_SMART_INFO_VISIBLE                       = SH.getKeyHandler("OverviewPanelSmartInfoVisible", BooleanKeyHandler.class);

    public static final StringKeyHandler                               ACTIVE_CONFIG_PANEL                                     = SH.getKeyHandler("ActiveConfigPanel", StringKeyHandler.class);

    public static final StringKeyHandler                               CUSTOM_LOOK_AND_FEEL_CLASS                              = SH.getKeyHandler("CustomLookAndFeelClass", StringKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE                = SH.getKeyHandler("OverviewPanelVisibleOnlyInfoVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              HELP_DIALOGS_ENABLED                                    = SH.getKeyHandler("HelpDialogsEnabled", BooleanKeyHandler.class);

    /**
     * Placeholders: |#TITLE|, | - #SPEED/s|, | - #UPDATENOTIFY|, | - #AVGSPEED|, | - #RUNNING_DOWNLOADS|
     **/
    public static final StringKeyHandler                               TITLE_PATTERN                                           = SH.getKeyHandler("TitlePattern", StringKeyHandler.class);

    public static final EnumKeyHandler                                 LINKGRABBER_BOTTOMBAR_POSITION                          = SH.getKeyHandler("LinkgrabberBottombarPosition", EnumKeyHandler.class);

    /**
     * The last used the Regex option for 'Rename Filename/Packagename' Dialog
     **/
    public static final BooleanKeyHandler                              RENAME_ACTION_REGEX_ENABLED                             = SH.getKeyHandler("RenameActionRegexEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 LINK_DOUBLE_CLICK_ACTION                                = SH.getKeyHandler("LinkDoubleClickAction", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_SPEED_VISIBLE                   = SH.getKeyHandler("OverviewPanelDownloadSpeedVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_TOTAL_BYTES_VISIBLE          = SH.getKeyHandler("OverviewPanelLinkgrabberTotalBytesVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_BYTES_LOADED_VISIBLE            = SH.getKeyHandler("OverviewPanelDownloadBytesLoadedVisible", BooleanKeyHandler.class);

    /**
     * The row that is 'touched' by the mouse cursor gets a darker shadow
     **/
    public static final BooleanKeyHandler                              TABLE_MOUSE_OVER_HIGHLIGHT_ENABLED                      = SH.getKeyHandler("TableMouseOverHighlightEnabled", BooleanKeyHandler.class);

    public static final StringKeyHandler                               DATE_TIME_FORMAT_ACCOUNT_MANAGER_EXPIRE_DATE_COLUMN     = SH.getKeyHandler("DateTimeFormatAccountManagerExpireDateColumn", StringKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_STATUS_ONLINE_VISIBLE        = SH.getKeyHandler("OverviewPanelLinkgrabberStatusOnlineVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              MY_JDOWNLOADER_VIEW_VISIBLE                             = SH.getKeyHandler("MyJDownloaderViewVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 MAC_DOCK_PROGRESS_DISPLAY                               = SH.getKeyHandler("MacDockProgressDisplay", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              SPECIAL_DEAL_OBOOM_DIALOG_VISIBLE_ON_STARTUP            = SH.getKeyHandler("SpecialDealOboomDialogVisibleOnStartup", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_LINK_COUNT_VISIBLE              = SH.getKeyHandler("OverviewPanelDownloadLinkCountVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 SELECTED_DOWNLOAD_SEARCH_CATEGORY                       = SH.getKeyHandler("SelectedDownloadSearchCategory", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 CONFIRM_INCOMPLETE_ARCHIVE_ACTION                       = SH.getKeyHandler("ConfirmIncompleteArchiveAction", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 LAST_IF_FILE_EXISTS                                     = SH.getKeyHandler("LastIfFileExists", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE      = SH.getKeyHandler("DownloadsPropertiesPanelArchivepasswordVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              UPDATE_BUTTON_FLASHING_ENABLED                          = SH.getKeyHandler("UpdateButtonFlashingEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              BALLOON_NOTIFICATION_ENABLED                            = SH.getKeyHandler("BalloonNotificationEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE                  = SH.getKeyHandler("LinkPropertiesPanelChecksumVisible", BooleanKeyHandler.class);

    /**
     * Set to true of you want jd to remember the latest selected download view
     **/
    public static final BooleanKeyHandler                              SAVE_DOWNLOAD_VIEW_CROSS_SESSION_ENABLED                = SH.getKeyHandler("SaveDownloadViewCrossSessionEnabled", BooleanKeyHandler.class);

    /**
     * If enabled ctrl+A first of all selects all children in all current packages, and in a second step all packages
     **/
    public static final BooleanKeyHandler                              TWO_STEP_CTRL_ASELECTION_ENABLED                        = SH.getKeyHandler("TwoStepCtrlASelectionEnabled", BooleanKeyHandler.class);

    /**
     * Enable/Disable the Downloads properties panel
     **/
    public static final BooleanKeyHandler                              DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE                  = SH.getKeyHandler("DownloadsTabPropertiesPanelVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              TABLE_WRAP_AROUND_ENABLED                               = SH.getKeyHandler("TableWrapAroundEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 DOWNLOAD_VIEW                                           = SH.getKeyHandler("DownloadView", EnumKeyHandler.class);

    /**
     * This value is read from the windows registry. if you set it, JDownloader will write it back to the registry.
     **/
    public static final IntegerKeyHandler                              WINDOWS_WINDOW_MANAGER_FOREGROUND_LOCK_TIMEOUT          = SH.getKeyHandler("WindowsWindowManagerForegroundLockTimeout", IntegerKeyHandler.class);

    /**
     * JDownloader uses a workaround to bring it's window or dialogs to focused to front. It simulates an ALT key shortcut. If disabled, you
     * will get focus problems
     **/
    public static final ListHandler<int[]>                             WINDOWS_WINDOW_MANAGER_ALT_KEY_COMBI                    = SH.getKeyHandler("WindowsWindowManagerAltKeyCombi", ListHandler.class);

    public static final StringKeyHandler                               DONATION_NOTIFY_ID                                      = SH.getKeyHandler("DonationNotifyID", StringKeyHandler.class);

    /**
     * Enable/disable HTML-Flavor(Browser selection) Clipboard monitoring
     **/
    public static final BooleanKeyHandler                              CLIPBOARD_MONITOR_PROCESS_HTMLFLAVOR                    = SH.getKeyHandler("ClipboardMonitorProcessHTMLFlavor", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_VISIBLE                             = SH.getKeyHandler("LinkgrabberSidebarVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_SELECTED_INFO_VISIBLE                    = SH.getKeyHandler("OverviewPanelSelectedInfoVisible", BooleanKeyHandler.class);

    /**
     * If true, java will try to use D3D for graphics
     **/
    public static final BooleanKeyHandler                              USE_D3D                                                 = SH.getKeyHandler("UseD3D", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_LINKS_FAILED_COUNT_VISIBLE      = SH.getKeyHandler("OverviewPanelDownloadLinksFailedCountVisible", BooleanKeyHandler.class);

    /**
     * Include disabled links in the totalbytes and loadedbytes calculation
     **/
    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS    = SH.getKeyHandler("OverviewPanelDownloadPanelIncludeDisabledLinks", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_LINKS_COUNT_VISIBLE          = SH.getKeyHandler("OverviewPanelLinkgrabberLinksCountVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              PREMIUM_EXPIRE_WARNING_ENABLED                          = SH.getKeyHandler("PremiumExpireWarningEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              SPEED_METER_VISIBLE                                     = SH.getKeyHandler("SpeedMeterVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              SPECIAL_DEALS_ENABLED                                   = SH.getKeyHandler("SpecialDealsEnabled", BooleanKeyHandler.class);

    public static final ListHandler<int[]>                             DOWNLOAD_LIST_SCROLL_POSITION                           = SH.getKeyHandler("DownloadListScrollPosition", ListHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE        = SH.getKeyHandler("DownloadsPropertiesPanelDownloadFromVisible", BooleanKeyHandler.class);

    /**
     * JDownloader uses a workaround to bring it's window or dialogs to focused to front. It simulates an ALT key shortcut. If disabled, you
     * will get focus problems
     **/
    public static final BooleanKeyHandler                              WINDOWS_WINDOW_MANAGER_ALT_KEY_WORKAROUND_ENABLED       = SH.getKeyHandler("WindowsWindowManagerAltKeyWorkaroundEnabled", BooleanKeyHandler.class);

    /**
     * Captcha Dialog Image scale Faktor in %
     **/
    public static final IntegerKeyHandler                              CAPTCHA_SCALE_FACTOR                                    = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);

    /**
     * Highlight Column in Downloadview if table is not in downloadsortorder
     **/
    public static final BooleanKeyHandler                              SORT_COLUMN_HIGHLIGHT_ENABLED                           = SH.getKeyHandler("SortColumnHighlightEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                               PREMIUM_EXPIRE_WARNING_MAP_V2                           = SH.getKeyHandler("PremiumExpireWarningMapV2", ObjectKeyHandler.class);

    /**
     * If true, hostcolumn will also show full hostname
     **/
    public static final BooleanKeyHandler                              SHOW_FULL_HOSTNAME                                      = SH.getKeyHandler("ShowFullHostname", BooleanKeyHandler.class);

    /**
     * Use horizontal Scrollbars in DownloadTable
     **/
    public static final BooleanKeyHandler                              HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED         = SH.getKeyHandler("HorizontalScrollbarsInDownloadTableEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 LOOK_AND_FEEL_THEME                                     = SH.getKeyHandler("LookAndFeelTheme", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 DOWNLOAD_FOLDER_CHOOSER_DEFAULT_PATH                    = SH.getKeyHandler("DownloadFolderChooserDefaultPath", EnumKeyHandler.class);

    public static final StringKeyHandler                               PASSWORD                                                = SH.getKeyHandler("Password", StringKeyHandler.class);

    /**
     * If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_SPEED_COLUMN_ENABLED                      = SH.getKeyHandler("PremiumAlertSpeedColumnEnabled", BooleanKeyHandler.class);

    /**
     * Include disabled links in the size calculation
     **/
    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_INCLUDE_DISABLED_LINKS       = SH.getKeyHandler("OverviewPanelLinkgrabberIncludeDisabledLinks", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                              SPEED_METER_FRAMES_PER_SECOND                           = SH.getKeyHandler("SpeedMeterFramesPerSecond", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE               = SH.getKeyHandler("LinkPropertiesPanelPackagenameVisible", BooleanKeyHandler.class);

    /**
     * Shortcut for the Refresh Button in the captcha dialog
     **/
    public static final StringKeyHandler                               SHORTCUT_FOR_CAPTCHA_DIALOG_REFRESH                     = SH.getKeyHandler("ShortcutForCaptchaDialogRefresh", StringKeyHandler.class);

    /**
     * Set to false to invert the sort Order for the Download & Linkgrabber Tables.
     **/
    public static final BooleanKeyHandler                              PRIMARY_TABLE_SORTER_DESC                               = SH.getKeyHandler("PrimaryTableSorterDesc", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_PACKAGE_COUNT_VISIBLE        = SH.getKeyHandler("OverviewPanelLinkgrabberPackageCountVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_CHECKSUM_VISIBLE             = SH.getKeyHandler("DownloadsPropertiesPanelChecksumVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_STATUS_OFFLINE_VISIBLE       = SH.getKeyHandler("OverviewPanelLinkgrabberStatusOfflineVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              HATE_CAPTCHAS_TEXT_IN_CAPTCHA_DIALOG_VISIBLE            = SH.getKeyHandler("HateCaptchasTextInCaptchaDialogVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              STATUS_BAR_ADD_PREMIUM_BUTTON_VISIBLE                   = SH.getKeyHandler("StatusBarAddPremiumButtonVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE          = SH.getKeyHandler("DownloadsPropertiesPanelPackagenameVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOAD_CONTROL_COLUMN_AUTO_SHOW_ENABLED               = SH.getKeyHandler("DownloadControlColumnAutoShowEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              PROPERTIES_PANEL_HEIGHT_NORMALIZED                      = SH.getKeyHandler("PropertiesPanelHeightNormalized", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              ULBANNER_ENABLED                                        = SH.getKeyHandler("ULBannerEnabled", BooleanKeyHandler.class);

    public static final LongKeyHandler                                 DONATE_BUTTON_LATEST_AUTO_CHANGE                        = SH.getKeyHandler("DonateButtonLatestAutoChange", LongKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_FILENAME_VISIBLE                  = SH.getKeyHandler("LinkPropertiesPanelFilenameVisible", BooleanKeyHandler.class);
}