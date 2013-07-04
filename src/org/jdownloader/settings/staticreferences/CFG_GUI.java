package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
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
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                                   = (StorageHandler<GraphicalUserInterfaceSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // MAINFRAME_IS_MAXIMIZED
    /**
     * Action that will be performed when the Linkgrabber adds new links.
     **/
    public static final EnumKeyHandler                                 MAINFRAME_POPUP_TRIGGER_WHEN_NEW_LINKS_WERE_ADDED    = SH.getKeyHandler("MainframePopupTriggerWhenNewLinksWereAdded", EnumKeyHandler.class);
    // false
    /**
     * Paint all labels/text with or without antialias. Default value is false.
     **/
    public static final BooleanKeyHandler                              SPEEDMETER_ANTI_ALIASING_ENABLED                     = SH.getKeyHandler("SpeedmeterAntiAliasingEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              TASK_BAR_FLASH_ENABLED                               = SH.getKeyHandler("TaskBarFlashEnabled", BooleanKeyHandler.class);
    // 500
    /**
     * Refreshrate in ms for the DownloadView
     **/
    public static final LongKeyHandler                                 DOWNLOAD_VIEW_REFRESH                                = SH.getKeyHandler("DownloadViewRefresh", LongKeyHandler.class);
    // false
    /**
     * If true, java will try to use D3D for graphics
     **/
    public static final BooleanKeyHandler                              USE_D3D                                              = SH.getKeyHandler("UseD3D", BooleanKeyHandler.class);
    // false
    /**
     * Enable/disable Enable/disable Clipboard monitoring
     **/
    public static final BooleanKeyHandler                              CLIPBOARD_MONITORED                                  = SH.getKeyHandler("ClipboardMonitored", BooleanKeyHandler.class);
    // false
    /**
     * Use horizontal Scrollbars in Linkgrabber
     **/
    public static final BooleanKeyHandler                              HORIZONTAL_SCROLLBARS_IN_LINKGRABBER_TABLE_ENABLED   = SH.getKeyHandler("HorizontalScrollbarsInLinkgrabberTableEnabled", BooleanKeyHandler.class);
    // jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings
    public static final StringKeyHandler                               ACTIVE_CONFIG_PANEL                                  = SH.getKeyHandler("ActiveConfigPanel", StringKeyHandler.class);
    // null
    public static final StringKeyHandler                               ACTIVE_PLUGIN_CONFIG_PANEL                           = SH.getKeyHandler("ActivePluginConfigPanel", StringKeyHandler.class);
    // standard
    /**
     * Icon Theme ID. Make sure that ./themes/<ID>/ exists
     **/
    public static final StringKeyHandler                               THEME_ID                                             = SH.getKeyHandler("ThemeID", StringKeyHandler.class);
    // false
    /**
     * If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_TASK_COLUMN_ENABLED                    = SH.getKeyHandler("PremiumAlertTaskColumnEnabled", BooleanKeyHandler.class);
    // CLEAR_LIST
    public static final EnumKeyHandler                                 LINKGRABBER_DEFAULT_CLEAR_ACTION                     = SH.getKeyHandler("LinkgrabberDefaultClearAction", EnumKeyHandler.class);
    // true
    /**
     * Enable/Disable the DownloadPanel Overview panel
     **/
    public static final BooleanKeyHandler                              DOWNLOAD_PANEL_OVERVIEW_VISIBLE                      = SH.getKeyHandler("DownloadPanelOverviewVisible", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              BALLOON_NOTIFICATION_ENABLED                         = SH.getKeyHandler("BalloonNotificationEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              OVERVIEW_PANEL_VISIBLE_ONLY_INFO_VISIBLE             = SH.getKeyHandler("OverviewPanelVisibleOnlyInfoVisible", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              CONFIG_VIEW_VISIBLE                                  = SH.getKeyHandler("ConfigViewVisible", BooleanKeyHandler.class);
    // null
    public static final StringKeyHandler                               LOOK_AND_FEEL                                        = SH.getKeyHandler("LookAndFeel", StringKeyHandler.class);
    // SKIP_FILE
    public static final EnumKeyHandler                                 LAST_IF_FILE_EXISTS                                  = SH.getKeyHandler("LastIfFileExists", EnumKeyHandler.class);
    // 100
    /**
     * Captcha Dialog Image scale Faktor in %
     **/
    public static final IntegerKeyHandler                              CAPTCHA_SCALE_FACTOR                                 = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);
    // FILENAME
    public static final EnumKeyHandler                                 SELECTED_DOWNLOAD_SEARCH_CATEGORY                    = SH.getKeyHandler("SelectedDownloadSearchCategory", EnumKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              PASSWORD_PROTECTION_ENABLED                          = SH.getKeyHandler("PasswordProtectionEnabled", BooleanKeyHandler.class);
    // false
    /**
     * Set to true of you want jd to remember the latest selected download view
     **/
    public static final BooleanKeyHandler                              SAVE_DOWNLOAD_VIEW_CROSS_SESSION_ENABLED             = SH.getKeyHandler("SaveDownloadViewCrossSessionEnabled", BooleanKeyHandler.class);
    // HIDE_IF_CTRL_IS_PRESSED_AND_NEVER_DELETE_ANY_LINKS_FROM_HARDDISK
    public static final EnumKeyHandler                                 SHOW_DELETE_LINKS_DIALOG_OPTION                      = SH.getKeyHandler("ShowDeleteLinksDialogOption", EnumKeyHandler.class);
    // null
    public static final StringKeyHandler                               PASSWORD                                             = SH.getKeyHandler("Password", StringKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              CAPTCHA_DIALOG_UNIQUE_POSITION_BY_HOSTER_ENABLED     = SH.getKeyHandler("CaptchaDialogUniquePositionByHosterEnabled", BooleanKeyHandler.class);
    // true
    /**
     * If enabled, JDownloader GUI switch to Linkgrabber Tab when new links are added
     **/
    public static final BooleanKeyHandler                              SWITCH_TO_LINKGRABBER_TAB_ON_NEW_LINKS_ADDED_ENABLED = SH.getKeyHandler("SwitchToLinkgrabberTabOnNewLinksAddedEnabled", BooleanKeyHandler.class);
    // DETAILS
    public static final EnumKeyHandler                                 FILE_CHOOSER_VIEW                                    = SH.getKeyHandler("FileChooserView", EnumKeyHandler.class);
    // true
    /**
     * If enabled, The User Interface will switch to Linkgrabber Tab if a new job has been added
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_AUTO_TAB_SWITCH_ENABLED                  = SH.getKeyHandler("LinkgrabberAutoTabSwitchEnabled", BooleanKeyHandler.class);
    // false
    /**
     * Enable/Disable the Linkgrabber Overview panel
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_OVERVIEW_VISIBLE                         = SH.getKeyHandler("LinkgrabberOverviewVisible", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              FILE_COUNT_IN_SIZE_COLUMN_VISIBLE                    = SH.getKeyHandler("FileCountInSizeColumnVisible", BooleanKeyHandler.class);
    // true
    /**
     * Enable/Disable the Linkgrabber Sidebar
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_ENABLED                          = SH.getKeyHandler("LinkgrabberSidebarEnabled", BooleanKeyHandler.class);
    // 0
    /**
     * by default, table row's height dynamicly adapts to the fontsize. Set a value>0 to set your own custom row height.
     **/
    public static final IntegerKeyHandler                              CUSTOM_TABLE_ROW_HEIGHT                              = SH.getKeyHandler("CustomTableRowHeight", IntegerKeyHandler.class);
    // ADD_ALL_LINKS_AND_START_DOWNLOADS
    /**
     * Choose what should happen when you click on the [Start Downloads] Button when you are in the Linkgrabber Tab
     **/
    public static final EnumKeyHandler                                 START_BUTTON_ACTION_IN_LINKGRABBER_CONTEXT           = SH.getKeyHandler("StartButtonActionInLinkgrabberContext", EnumKeyHandler.class);
    // 2000
    public static final IntegerKeyHandler                              TOOLTIP_DELAY                                        = SH.getKeyHandler("TooltipDelay", IntegerKeyHandler.class);
    // MAINFRAME_IS_MAXIMIZED
    /**
     * Action that will be performed when the Linkgrabber adds new links.
     **/
    public static final EnumKeyHandler                                 FOCUS_TRIGGER_FOR_CAPTCHA_DIALOGS                    = SH.getKeyHandler("FocusTriggerForCaptchaDialogs", EnumKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              SKIP_CLIPBOARD_MONITOR_FIRST_ROUND                   = SH.getKeyHandler("SkipClipboardMonitorFirstRound", BooleanKeyHandler.class);
    // true
    /**
     * Enable/Disable the Linkgrabber Sidebar QuicktoggleButton
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_TOGGLE_BUTTON_ENABLED            = SH.getKeyHandler("LinkgrabberSidebarToggleButtonEnabled", BooleanKeyHandler.class);
    // false
    /**
     * If disabled, The Hostercolumn will show gray disabled icons if the link is disabled
     **/
    public static final BooleanKeyHandler                              COLORED_ICONS_FOR_DISABLED_HOSTER_COLUMN_ENABLED     = SH.getKeyHandler("ColoredIconsForDisabledHosterColumnEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              DOWNLOAD_PANEL_OVERVIEW_SETTINGS_VISIBLE             = SH.getKeyHandler("DownloadPanelOverviewSettingsVisible", BooleanKeyHandler.class);
    // ALL
    public static final EnumKeyHandler                                 DOWNLOAD_VIEW                                        = SH.getKeyHandler("DownloadView", EnumKeyHandler.class);
    // false
    /**
     * If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_ETACOLUMN_ENABLED                      = SH.getKeyHandler("PremiumAlertETAColumnEnabled", BooleanKeyHandler.class);
    // false
    /**
     * Requirment: Java 1.7 / Set to true if you want JDownloader to steal focus when the window pops up
     **/
    public static final BooleanKeyHandler                              WINDOWS_REQUEST_FOCUS_ON_ACTIVATION_ENABLED          = SH.getKeyHandler("WindowsRequestFocusOnActivationEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Highlight Column in Downloadview if table is not in downloadsortorder
     **/
    public static final BooleanKeyHandler                              SORT_COLUMN_HIGHLIGHT_ENABLED                        = SH.getKeyHandler("SortColumnHighlightEnabled", BooleanKeyHandler.class);
    // false
    /**
     * If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_SPEED_COLUMN_ENABLED                   = SH.getKeyHandler("PremiumAlertSpeedColumnEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              LOG_VIEW_VISIBLE                                     = SH.getKeyHandler("LogViewVisible", BooleanKeyHandler.class);
    // false
    /**
     * Hide the package in case it only contains one child
     **/
    public static final BooleanKeyHandler                              HIDE_SINGLE_CHILD_PACKAGES                           = SH.getKeyHandler("HideSingleChildPackages", BooleanKeyHandler.class);
    // false
    /**
     * Use horizontal Scrollbars in DownloadTable
     **/
    public static final BooleanKeyHandler                              HORIZONTAL_SCROLLBARS_IN_DOWNLOAD_TABLE_ENABLED      = SH.getKeyHandler("HorizontalScrollbarsInDownloadTableEnabled", BooleanKeyHandler.class);
    // null
    public static final ObjectKeyHandler                               LAST_FRAME_STATUS                                    = SH.getKeyHandler("LastFrameStatus", ObjectKeyHandler.class);
    // true
    /**
     * Highlight Table in Downloadview if table is filtered
     **/
    public static final BooleanKeyHandler                              FILTER_HIGHLIGHT_ENABLED                             = SH.getKeyHandler("FilterHighlightEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              OVERVIEW_PANEL_SMART_INFO_VISIBLE                    = SH.getKeyHandler("OverviewPanelSmartInfoVisible", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              OVERVIEW_PANEL_TOTAL_INFO_VISIBLE                    = SH.getKeyHandler("OverviewPanelTotalInfoVisible", BooleanKeyHandler.class);
    // false
    /**
     * If true, hostcolumn will also show full hostname
     **/
    public static final BooleanKeyHandler                              SHOW_FULL_HOSTNAME                                   = SH.getKeyHandler("ShowFullHostname", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_VISIBLE                          = SH.getKeyHandler("LinkgrabberSidebarVisible", BooleanKeyHandler.class);
    // 20000
    public static final IntegerKeyHandler                              DIALOG_DEFAULT_TIMEOUT_IN_MS                         = SH.getKeyHandler("DialogDefaultTimeoutInMS", IntegerKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              OVERVIEW_PANEL_SELECTED_INFO_VISIBLE                 = SH.getKeyHandler("OverviewPanelSelectedInfoVisible", BooleanKeyHandler.class);
    // true
    /**
     * If false, Most of the Tooltips will be disabled
     **/
    public static final BooleanKeyHandler                              TOOLTIP_ENABLED                                      = SH.getKeyHandler("TooltipEnabled", BooleanKeyHandler.class);
    // TOTAL_PROGRESS
    public static final EnumKeyHandler                                 MAC_DOCK_PROGRESS_DISPLAY                            = SH.getKeyHandler("MacDockProgressDisplay", EnumKeyHandler.class);
}