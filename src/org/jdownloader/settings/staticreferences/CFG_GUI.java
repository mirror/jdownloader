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
    public static final GraphicalUserInterfaceSettings                 CFG                                              = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                               = (StorageHandler<GraphicalUserInterfaceSettings>) CFG.getStorageHandler();

    public static final BooleanKeyHandler                              HIDE_SINGLECHILD_PACKAGES                        = SH.getKeyHandler("HideSingleChildPackages", BooleanKeyHandler.class);
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // ALL
    public static final EnumKeyHandler                                 DOWNLOAD_VIEW                                    = SH.getKeyHandler("DownloadView", EnumKeyHandler.class);
    // false
    /**
     * If true, ETAColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_ETACOLUMN_ENABLED                  = SH.getKeyHandler("PremiumAlertETAColumnEnabled", BooleanKeyHandler.class);
    // 500
    /**
     * Refreshrate in ms for the DownloadView
     **/
    public static final LongKeyHandler                                 DOWNLOAD_VIEW_REFRESH                            = SH.getKeyHandler("DownloadViewRefresh", LongKeyHandler.class);
    // false
    /**
     * Enable/disable Enable/disable Clipboard monitoring
     **/
    public static final BooleanKeyHandler                              CLIPBOARD_MONITORED                              = SH.getKeyHandler("ClipboardMonitored", BooleanKeyHandler.class);
    // jd.gui.swing.jdgui.views.settings.panels.advanced.AdvancedSettings
    public static final StringKeyHandler                               ACTIVE_CONFIG_PANEL                              = SH.getKeyHandler("ActiveConfigPanel", StringKeyHandler.class);
    // true
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_DOWN_BUTTON                            = SH.getKeyHandler("ShowMoveDownButton", BooleanKeyHandler.class);
    // true
    /**
     * Enable/disable support for system DPI settings. Default value is true.
     **/
    public static final BooleanKeyHandler                              FONT_RESPECTS_SYSTEM_DPI                         = SH.getKeyHandler("FontRespectsSystemDPI", BooleanKeyHandler.class);
    // standard
    /**
     * Icon Theme ID. Make sure that ./themes/<ID>/ exists
     **/
    public static final StringKeyHandler                               THEME_ID                                         = SH.getKeyHandler("ThemeID", StringKeyHandler.class);
    // true
    /**
     * Enable/Disable the Linkgrabber Sidebar QuicktoggleButton
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_TOGGLE_BUTTON_ENABLED        = SH.getKeyHandler("LinkgrabberSidebarToggleButtonEnabled", BooleanKeyHandler.class);
    // 100
    /**
     * Font scale factor in percent. Default value is 100 which means no font scaling.
     **/
    public static final IntegerKeyHandler                              FONT_SCALE_FACTOR                                = SH.getKeyHandler("FontScaleFactor", IntegerKeyHandler.class);
    // false
    /**
     * If true, TaskColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_TASK_COLUMN_ENABLED                = SH.getKeyHandler("PremiumAlertTaskColumnEnabled", BooleanKeyHandler.class);
    // false
    /**
     * Paint all labels/text with or without antialias. Default value is false.
     **/
    public static final BooleanKeyHandler                              TEXT_ANTI_ALIAS_ENABLED                          = SH.getKeyHandler("TextAntiAliasEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Highlight Column in Downloadview if table is not in downloadsortorder
     **/
    public static final BooleanKeyHandler                              SORT_COLUMN_HIGHLIGHT_ENABLED                    = SH.getKeyHandler("SortColumnHighlightEnabled", BooleanKeyHandler.class);
    // true
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_TO_BOTTOM_BUTTON                       = SH.getKeyHandler("ShowMoveToBottomButton", BooleanKeyHandler.class);
    // false
    /**
     * If true, SpeedColumn will show Premium Alerts in Free Download mode if JD thinks Premium would be better currently.
     **/
    public static final BooleanKeyHandler                              PREMIUM_ALERT_SPEED_COLUMN_ENABLED               = SH.getKeyHandler("PremiumAlertSpeedColumnEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              BALLOON_NOTIFICATION_ENABLED                     = SH.getKeyHandler("BalloonNotificationEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              LOG_VIEW_VISIBLE                                 = SH.getKeyHandler("LogViewVisible", BooleanKeyHandler.class);
    // null
    public static final StringKeyHandler                               LOOK_AND_FEEL                                    = SH.getKeyHandler("LookAndFeel", StringKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              CONFIG_VIEW_VISIBLE                              = SH.getKeyHandler("ConfigViewVisible", BooleanKeyHandler.class);
    // SKIP_FILE
    public static final EnumKeyHandler                                 LAST_IF_FILE_EXISTS                              = SH.getKeyHandler("LastIfFileExists", EnumKeyHandler.class);
    // 100
    /**
     * Captcha Dialog Image scale Faktor in %
     **/
    public static final IntegerKeyHandler                              CAPTCHA_SCALE_FACTOR                             = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);
    // false
    public static final BooleanKeyHandler                              PASSWORD_PROTECTION_ENABLED                      = SH.getKeyHandler("PasswordProtectionEnabled", BooleanKeyHandler.class);
    // FILENAME
    public static final EnumKeyHandler                                 SELECTED_DOWNLOAD_SEARCH_CATEGORY                = SH.getKeyHandler("SelectedDownloadSearchCategory", EnumKeyHandler.class);
    // true
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_TO_TOP_BUTTON                          = SH.getKeyHandler("ShowMoveToTopButton", BooleanKeyHandler.class);
    // false
    /**
     * Set to true of you want jd to remember the latest selected download view
     **/
    public static final BooleanKeyHandler                              SAVE_DOWNLOAD_VIEW_CROSS_SESSION_ENABLED         = SH.getKeyHandler("SaveDownloadViewCrossSessionEnabled", BooleanKeyHandler.class);
    // default
    /**
     * Font to be used. Default value is default.
     **/
    public static final StringKeyHandler                               FONT_NAME                                        = SH.getKeyHandler("FontName", StringKeyHandler.class);
    // null
    public static final ObjectKeyHandler                               LAST_FRAME_STATUS                                = SH.getKeyHandler("LastFrameStatus", ObjectKeyHandler.class);
    // null
    public static final StringKeyHandler                               PASSWORD                                         = SH.getKeyHandler("Password", StringKeyHandler.class);
    // true
    /**
     * Highlight Table in Downloadview if table is filtered
     **/
    public static final BooleanKeyHandler                              FILTER_HIGHLIGHT_ENABLED                         = SH.getKeyHandler("FilterHighlightEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Enable/Disable the Linkgrabber Sidebar
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_ENABLED                      = SH.getKeyHandler("LinkgrabberSidebarEnabled", BooleanKeyHandler.class);
    // 20
    public static final IntegerKeyHandler                              DIALOG_DEFAULT_TIMEOUT                           = SH.getKeyHandler("DialogDefaultTimeout", IntegerKeyHandler.class);
    // true
    /**
     * If enabled, The User Interface will switch to Linkgrabber Tab if a new job has been added
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_AUTO_TAB_SWITCH_ENABLED              = SH.getKeyHandler("LinkgrabberAutoTabSwitchEnabled", BooleanKeyHandler.class);
    // false
    /**
     * If enabled, JDownloader GUI will come to top when new links are added
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_FRAME_TO_TOP_ON_NEW_LINKS_ENABLED    = SH.getKeyHandler("LinkgrabberFrameToTopOnNewLinksEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_VISIBLE                      = SH.getKeyHandler("LinkgrabberSidebarVisible", BooleanKeyHandler.class);
    // true
    /**
     * If false, Most of the Tooltips will be disabled
     **/
    public static final BooleanKeyHandler                              TOOLTIP_ENABLED                                  = SH.getKeyHandler("TooltipEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Disable animation and all animation threads. Optional value. Default value is true.
     **/
    public static final BooleanKeyHandler                              ANIMATION_ENABLED                                = SH.getKeyHandler("AnimationEnabled", BooleanKeyHandler.class);
    // false
    /**
     * If disabled, The Hostercolumn will show gray disabled icons if the link is disabled
     **/
    public static final BooleanKeyHandler                              COLORED_ICONS_FOR_DISABLED_HOSTER_COLUMN_ENABLED = SH.getKeyHandler("ColoredIconsForDisabledHosterColumnEnabled", BooleanKeyHandler.class);

    /**
     * If true, The Hostercolumn will show full hostname
     **/
    public static final BooleanKeyHandler                              SHOW_FULL_HOSTNAME                               = SH.getKeyHandler("ShowFullHostname", BooleanKeyHandler.class);
    // true
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_UP_BUTTON                              = SH.getKeyHandler("ShowMoveUpButton", BooleanKeyHandler.class);
    // false
    /**
     * Enable/disable window opacity on Java 6u10 and above. A value of 'false' disables window opacity which means that the window corner background which is
     * visible for non-rectangular windows disappear. Furthermore the shadow for popupMenus makes use of real translucent window. Some themes like
     * SyntheticaSimple2D support translucent titlePanes if opacity is disabled. The property is ignored on JRE's below 6u10. Note: It is recommended to
     * activate this feature only if your graphics hardware acceleration is supported by the JVM - a value of 'false' can affect application performance.
     * Default value is false which means the translucency feature is enabled
     **/
    public static final BooleanKeyHandler                              WINDOW_OPAQUE                                    = SH.getKeyHandler("WindowOpaque", BooleanKeyHandler.class);
}