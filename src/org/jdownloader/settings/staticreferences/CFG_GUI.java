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
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class CFG_GUI {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(GraphicalUserInterfaceSettings.class);
    }

    // Static Mappings for interface org.jdownloader.settings.GraphicalUserInterfaceSettings
    public static final GraphicalUserInterfaceSettings                 CFG                                                  = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                                   = (StorageHandler<GraphicalUserInterfaceSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * This value is read from the windows registry. if you set it, JDownloader will write it back to the registry.
     **/
    public static final IntegerKeyHandler                              WINDOWS_WINDOW_MANAGER_FOREGROUND_LOCK_TIMEOUT       = SH.getKeyHandler("WindowsWindowManagerForegroundLockTimeout", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                              TASK_BAR_FLASH_ENABLED                               = SH.getKeyHandler("TaskBarFlashEnabled", BooleanKeyHandler.class);

    /**
     * Paint all labels/text with or without antialias. Default value is false.
     **/
    public static final BooleanKeyHandler                              SPEEDMETER_ANTI_ALIASING_ENABLED                     = SH.getKeyHandler("SpeedmeterAntiAliasingEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 DELETE_DIALOG_DEFAULT_SELECTION                      = SH.getKeyHandler("DeleteDialogDefaultSelection", EnumKeyHandler.class);

    /**
     * Refreshrate in ms for the DownloadView
     **/
    public static final LongKeyHandler                                 DOWNLOAD_VIEW_REFRESH                                = SH.getKeyHandler("DownloadViewRefresh", LongKeyHandler.class);

    public static final EnumKeyHandler                                 NEW_LINKS_ACTION                                     = SH.getKeyHandler("NewLinksAction", EnumKeyHandler.class);

    /**
     * Enable/disable Enable/disable Clipboard monitoring
     **/
    public static final BooleanKeyHandler                              CLIPBOARD_MONITORED                                  = SH.getKeyHandler("ClipboardMonitored", BooleanKeyHandler.class);

    /**
     * If true, java will try to use D3D for graphics
     **/
    public static final BooleanKeyHandler                              USE_D3D                                              = SH.getKeyHandler("UseD3D", BooleanKeyHandler.class);

    /**
     * Packages get a different background color if enabled
     **/
    public static final BooleanKeyHandler                              PACKAGES_BACKGROUND_HIGHLIGHT_ENABLED                = SH.getKeyHandler("PackagesBackgroundHighlightEnabled", BooleanKeyHandler.class);

    /**
     * Use horizontal Scrollbars in Linkgrabber
     **/
    public static final BooleanKeyHandler                              HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED   = SH.getKeyHandler("HorizontalScrollbarsInLinkgrabberTableEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 NEW_DIALOG_FRAME_STATE                               = SH.getKeyHandler("NewDialogFrameState", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_CHECKSUM_VISIBLE               = SH.getKeyHandler("LinkPropertiesPanelChecksumVisible", BooleanKeyHandler.class);

    /**
     * Enable/Disable the DownloadPanel Overview panel
     **/
    public static final BooleanKeyHandler                              DOWNLOAD_TAB_OVERVIEW_VISIBLE                        = SH.getKeyHandler("DownloadTabOverviewVisible", BooleanKeyHandler.class);

    public static final StringKeyHandler                               ACTIVE_CONFIG_PANEL                                  = SH.getKeyHandler("ActiveConfigPanel", StringKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_COMMENT_VISIBLE                = SH.getKeyHandler("LinkPropertiesPanelCommentVisible", BooleanKeyHandler.class);

    public static final StringKeyHandler                               ACTIVE_PLUGIN_CONFIG_PANEL                           = SH.getKeyHandler("ActivePluginConfigPanel", StringKeyHandler.class);

    public static final BooleanKeyHandler                              CAPTCHA_DEBUG_MODE_ENABLED                           = SH.getKeyHandler("CaptchaDebugModeEnabled", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                              SPEED_METER_FRAMES_PER_SECOND                        = SH.getKeyHandler("SpeedMeterFramesPerSecond", IntegerKeyHandler.class);

    /**
     * Icon Theme ID. Make sure that ./themes/<ID>/ exists
     **/
    public static final StringKeyHandler                               THEME_ID                                             = SH.getKeyHandler("ThemeID", StringKeyHandler.class);

    /**
     * If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_TASK_COLUMN_ENABLED                    = SH.getKeyHandler("PremiumAlertTaskColumnEnabled", BooleanKeyHandler.class);

    /**
     * JDownloader uses a workaround to bring it's window or dialogs to focused to front. It simulates an ALT key shortcut. If disabled, you
     * will get focus problems
     **/
    public static final ListHandler<int[]>                             WINDOWS_WINDOW_MANAGER_ALT_KEY_COMBI                 = (ListHandler<int[]>) SH.getKeyHandler("WindowsWindowManagerAltKeyCombi", ListHandler.class);

    public static final BooleanKeyHandler                              BALLOON_NOTIFICATION_ENABLED                         = SH.getKeyHandler("BalloonNotificationEnabled", BooleanKeyHandler.class);

    /**
     * Enable/Disable the Linkgrabber Overview panel
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_TAB_OVERVIEW_VISIBLE                     = SH.getKeyHandler("LinkgrabberTabOverviewVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE   = SH.getKeyHandler("DownloadsPropertiesPanelArchivepasswordVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE      = SH.getKeyHandler("LinkPropertiesPanelDownloadPasswordVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              CONFIG_VIEW_VISIBLE                                  = SH.getKeyHandler("ConfigViewVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE             = SH.getKeyHandler("OverviewPanelVisibleOnlyInfoVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_CHECKSUM_VISIBLE          = SH.getKeyHandler("DownloadsPropertiesPanelChecksumVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 LAST_IF_FILE_EXISTS                                  = SH.getKeyHandler("LastIfFileExists", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_FILENAME_VISIBLE               = SH.getKeyHandler("LinkPropertiesPanelFilenameVisible", BooleanKeyHandler.class);

    /**
     * Captcha Dialog Image scale Faktor in %
     **/
    public static final IntegerKeyHandler                              CAPTCHA_SCALE_FACTOR                                 = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                              PASSWORD_PROTECTION_ENABLED                          = SH.getKeyHandler("PasswordProtectionEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 SELECTED_DOWNLOAD_SEARCH_CATEGORY                    = SH.getKeyHandler("SelectedDownloadSearchCategory", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 LOOK_AND_FEEL_THEME                                  = SH.getKeyHandler("LookAndFeelTheme", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 PACKAGE_DOUBLE_CLICK_ACTION                          = SH.getKeyHandler("PackageDoubleClickAction", EnumKeyHandler.class);

    /**
     * Set to true of you want jd to remember the latest selected download view
     **/
    public static final BooleanKeyHandler                              SAVE_DOWNLOAD_VIEW_CROSS_SESSION_ENABLED             = SH.getKeyHandler("SaveDownloadViewCrossSessionEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 LINK_DOUBLE_CLICK_ACTION                             = SH.getKeyHandler("LinkDoubleClickAction", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              UPDATE_BUTTON_FLASHING_ENABLED                       = SH.getKeyHandler("UpdateButtonFlashingEnabled", BooleanKeyHandler.class);

    /**
     * Placeholders: |#TITLE|, | - #SPEED/s|, | - #UPDATENOTIFY|
     **/
    public static final StringKeyHandler                               TITLE_PATTERN                                        = SH.getKeyHandler("TitlePattern", StringKeyHandler.class);

    public static final StringKeyHandler                               PASSWORD                                             = SH.getKeyHandler("Password", StringKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_DOWNLOAD_PANEL_INCLUDE_DISABLED_LINKS = SH.getKeyHandler("OverviewPanelDownloadPanelIncludeDisabledLinks", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_SAVE_TO_VISIBLE                = SH.getKeyHandler("LinkPropertiesPanelSaveToVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE          = SH.getKeyHandler("LinkPropertiesPanelDownloadFromVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              CAPTCHA_DIALOG_UNIQUE_POSITION_BY_HOSTER_ENABLED     = SH.getKeyHandler("CaptchaDialogUniquePositionByHosterEnabled", BooleanKeyHandler.class);

    /**
     * If enabled, JDownloader GUI switch to Linkgrabber Tab when new links are added
     **/
    public static final BooleanKeyHandler                              SWITCH_TO_LINKGRABBER_TAB_ON_NEW_LINKS_ADDED_ENABLED = SH.getKeyHandler("SwitchToLinkgrabberTabOnNewLinksAddedEnabled", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                              SPEED_METER_TIME_FRAME                               = SH.getKeyHandler("SpeedMeterTimeFrame", IntegerKeyHandler.class);

    public static final EnumKeyHandler                                 FILE_CHOOSER_VIEW                                    = SH.getKeyHandler("FileChooserView", EnumKeyHandler.class);

    /**
     * If enabled, The User Interface will switch to Linkgrabber Tab if a new job has been added
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_AUTO_TAB_SWITCH_ENABLED                  = SH.getKeyHandler("LinkgrabberAutoTabSwitchEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_FILENAME_VISIBLE          = SH.getKeyHandler("DownloadsPropertiesPanelFilenameVisible", BooleanKeyHandler.class);

    /**
     * Enable/Disable the Downloads properties panel
     **/
    public static final BooleanKeyHandler                              DOWNLOADS_TAB_PROPERTIES_PANEL_VISIBLE               = SH.getKeyHandler("DownloadsTabPropertiesPanelVisible", BooleanKeyHandler.class);

    /**
     * Enable/Disable the Linkgrabber properties panel
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_TAB_PROPERTIES_PANEL_VISIBLE             = SH.getKeyHandler("LinkgrabberTabPropertiesPanelVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              FILE_COUNT_IN_SIZE_COLUMN_VISIBLE                    = SH.getKeyHandler("FileCountInSizeColumnVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_SAVE_TO_VISIBLE           = SH.getKeyHandler("DownloadsPropertiesPanelSaveToVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_COMMENT_VISIBLE           = SH.getKeyHandler("DownloadsPropertiesPanelCommentVisible", BooleanKeyHandler.class);

    /**
     * The row that is 'touched' by the mouse cursor gets a darker shadow
     **/
    public static final BooleanKeyHandler                              TABLE_MOUSE_OVER_HIGHLIGHT_ENABLED                   = SH.getKeyHandler("TableMouseOverHighlightEnabled", BooleanKeyHandler.class);

    /**
     * by default, table row's height dynamicly adapts to the fontsize. Set a value>0 to set your own custom row height.
     **/
    public static final IntegerKeyHandler                              CUSTOM_TABLE_ROW_HEIGHT                              = SH.getKeyHandler("CustomTableRowHeight", IntegerKeyHandler.class);

    public static final IntegerKeyHandler                              TOOLTIP_DELAY                                        = SH.getKeyHandler("TooltipDelay", IntegerKeyHandler.class);

    /**
     * Choose what should happen when you click on the [Start Downloads] Button when you are in the Linkgrabber Tab
     **/
    public static final EnumKeyHandler                                 START_BUTTON_ACTION_IN_LINKGRABBER_CONTEXT           = SH.getKeyHandler("StartButtonActionInLinkgrabberContext", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 SELECTED_LINKGRABBER_SEARCH_CATEGORY                 = SH.getKeyHandler("SelectedLinkgrabberSearchCategory", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_FROM_VISIBLE     = SH.getKeyHandler("DownloadsPropertiesPanelDownloadFromVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_LINKGRABBER_INCLUDE_DISABLED_LINKS    = SH.getKeyHandler("OverviewPanelLinkgrabberIncludeDisabledLinks", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              SKIP_CLIPBOARD_MONITOR_FIRST_ROUND                   = SH.getKeyHandler("SkipClipboardMonitorFirstRound", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_PACKAGENAME_VISIBLE            = SH.getKeyHandler("LinkPropertiesPanelPackagenameVisible", BooleanKeyHandler.class);

    /**
     * If disabled, The Hostercolumn will show gray disabled icons if the link is disabled
     **/
    public static final BooleanKeyHandler                              COLORED_ICONS_FOR_DISABLED_HOSTER_COLUMN_ENABLED     = SH.getKeyHandler("ColoredIconsForDisabledHosterColumnEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE             = SH.getKeyHandler("DownloadPanelOverviewSettingsVisible", BooleanKeyHandler.class);

    /**
     * Every odd row get's a light shadow if enabled
     **/
    public static final BooleanKeyHandler                              TABLE_ALTERNATE_ROW_HIGHLIGHT_ENABLED                = SH.getKeyHandler("TableAlternateRowHighlightEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_PACKAGENAME_VISIBLE       = SH.getKeyHandler("DownloadsPropertiesPanelPackagenameVisible", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 DOWNLOAD_VIEW                                        = SH.getKeyHandler("DownloadView", EnumKeyHandler.class);

    /**
     * If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_ETACOLUMN_ENABLED                      = SH.getKeyHandler("PremiumAlertETAColumnEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              SPEED_METER_VISIBLE                                  = SH.getKeyHandler("SpeedMeterVisible", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LINK_PROPERTIES_PANEL_ARCHIVEPASSWORD_VISIBLE        = SH.getKeyHandler("LinkPropertiesPanelArchivepasswordVisible", BooleanKeyHandler.class);

    /**
     * Highlight Column in Downloadview if table is not in downloadsortorder
     **/
    public static final BooleanKeyHandler                              SORT_COLUMN_HIGHLIGHT_ENABLED                        = SH.getKeyHandler("SortColumnHighlightEnabled", BooleanKeyHandler.class);

    /**
     * If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_SPEED_COLUMN_ENABLED                   = SH.getKeyHandler("PremiumAlertSpeedColumnEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              LOG_VIEW_VISIBLE                                     = SH.getKeyHandler("LogViewVisible", BooleanKeyHandler.class);

    /**
     * Hide the package in case it only contains one child
     **/
    public static final BooleanKeyHandler                              HIDE_SINGLE_CHILD_PACKAGES                           = SH.getKeyHandler("HideSingleChildPackages", BooleanKeyHandler.class);

    /**
     * Use horizontal Scrollbars in DownloadTable
     **/
    public static final BooleanKeyHandler                              HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED      = SH.getKeyHandler("HorizontalScrollbarsInDownloadTableEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              DOWNLOADS_PROPERTIES_PANEL_DOWNLOAD_PASSWORD_VISIBLE = SH.getKeyHandler("DownloadsPropertiesPanelDownloadPasswordVisible", BooleanKeyHandler.class);

    /**
     * Highlight Table in Downloadview if table is filtered
     **/
    public static final BooleanKeyHandler                              FILTER_HIGHLIGHT_ENABLED                             = SH.getKeyHandler("FilterHighlightEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                               LAST_FRAME_STATUS                                    = SH.getKeyHandler("LastFrameStatus", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_SMART_INFO_VISIBLE                    = SH.getKeyHandler("OverviewPanelSmartInfoVisible", BooleanKeyHandler.class);

    /**
     * JDownloader uses a workaround to bring it's window or dialogs to focused to front. It simulates an ALT key shortcut. If disabled, you
     * will get focus problems
     **/
    public static final BooleanKeyHandler                              WINDOWS_WINDOW_MANAGER_ALT_KEY_WORKAROUND_ENABLED    = SH.getKeyHandler("WindowsWindowManagerAltKeyWorkaroundEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_TOTAL_INFO_VISIBLE                    = SH.getKeyHandler("OverviewPanelTotalInfoVisible", BooleanKeyHandler.class);

    /**
     * If true, hostcolumn will also show full hostname
     **/
    public static final BooleanKeyHandler                              SHOW_FULL_HOSTNAME                                   = SH.getKeyHandler("ShowFullHostname", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 PREMIUM_STATUS_BAR_DISPLAY                           = SH.getKeyHandler("PremiumStatusBarDisplay", EnumKeyHandler.class);

    public static final EnumKeyHandler                                 SPEED_IN_WINDOW_TITLE                                = SH.getKeyHandler("SpeedInWindowTitle", EnumKeyHandler.class);

    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_VISIBLE                          = SH.getKeyHandler("LinkgrabberSidebarVisible", BooleanKeyHandler.class);

    public static final IntegerKeyHandler                              DIALOG_DEFAULT_TIMEOUT_IN_MS                         = SH.getKeyHandler("DialogDefaultTimeoutInMS", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                              OVERVIEW_PANEL_SELECTED_INFO_VISIBLE                 = SH.getKeyHandler("OverviewPanelSelectedInfoVisible", BooleanKeyHandler.class);

    /**
     * If false, Most of the Tooltips will be disabled
     **/
    public static final BooleanKeyHandler                              TOOLTIP_ENABLED                                      = SH.getKeyHandler("TooltipEnabled", BooleanKeyHandler.class);

    public static final EnumKeyHandler                                 MAC_DOCK_PROGRESS_DISPLAY                            = SH.getKeyHandler("MacDockProgressDisplay", EnumKeyHandler.class);
}