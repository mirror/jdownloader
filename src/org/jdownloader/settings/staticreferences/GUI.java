package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.settings.GraphicalUserInterfaceSettings;

public class GUI {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(GraphicalUserInterfaceSettings.class);
    }

    // Static Mappings for interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings
    public static final GraphicalUserInterfaceSettings                 CFG                                       = JsonConfig.create(GraphicalUserInterfaceSettings.class);
    public static final StorageHandler<GraphicalUserInterfaceSettings> SH                                        = (StorageHandler<GraphicalUserInterfaceSettings>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.clipboardmonitored
    // = false
    /**
     * Enable/disable Enable/disable Clipboard monitoring
     **/
    public static final BooleanKeyHandler                              CLIPBOARD_MONITORED                       = SH.getKeyHandler("ClipboardMonitored", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.captchabackgroundcleanupenabled
    // = true
    /**
     * If enabled, the background of captchas will be removed to fit to the rest
     * of the design (transparency)
     **/
    public static final BooleanKeyHandler                              CAPTCHA_BACKGROUND_CLEANUP_ENABLED        = SH.getKeyHandler("CaptchaBackgroundCleanupEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.activeconfigpanel
    // = null
    public static final StringKeyHandler                               ACTIVE_CONFIG_PANEL                       = SH.getKeyHandler("ActiveConfigPanel", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.showmovedownbutton
    // = false
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_DOWN_BUTTON                     = SH.getKeyHandler("ShowMoveDownButton", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.fontrespectssystemdpi
    // = true
    /**
     * Enable/disable support for system DPI settings. Default value is true.
     **/
    public static final BooleanKeyHandler                              FONT_RESPECTS_SYSTEM_DPI                  = SH.getKeyHandler("FontRespectsSystemDPI", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.themeid =
    // standard
    /**
     * Icon Theme ID. Make sure that ./themes/<ID>/ exists
     **/
    public static final StringKeyHandler                               THEME_ID                                  = SH.getKeyHandler("ThemeID", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.linkgrabbersidebartogglebuttonenabled
    // = true
    /**
     * Enable/Disable the Linkgrabber Sidebar QuicktoggleButton
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_TOGGLE_BUTTON_ENABLED = SH.getKeyHandler("LinkgrabberSidebarToggleButtonEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.fontscalefactor =
    // 100
    /**
     * Font scale factor in percent. Default value is 100 which means no font
     * scaling.
     **/
    public static final IntegerKeyHandler                              FONT_SCALE_FACTOR                         = SH.getKeyHandler("FontScaleFactor", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.textantialiasenabled
    // = false
    /**
     * Paint all labels/text with or without antialias. Default value is false.
     **/
    public static final BooleanKeyHandler                              TEXT_ANTI_ALIAS_ENABLED                   = SH.getKeyHandler("TextAntiAliasEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.sortcolumnhighlightenabled
    // = true
    /**
     * Highlight Column in Downloadview if table is not in downloadsortorder
     **/
    public static final BooleanKeyHandler                              SORT_COLUMN_HIGHLIGHT_ENABLED             = SH.getKeyHandler("SortColumnHighlightEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.showmovetobottombutton
    // = false
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_TO_BOTTOM_BUTTON                = SH.getKeyHandler("ShowMoveToBottomButton", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.balloonnotificationenabled
    // = true
    public static final BooleanKeyHandler                              BALLOON_NOTIFICATION_ENABLED              = SH.getKeyHandler("BalloonNotificationEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.logviewvisible =
    // false
    public static final BooleanKeyHandler                              LOG_VIEW_VISIBLE                          = SH.getKeyHandler("LogViewVisible", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.configviewvisible
    // = false
    public static final BooleanKeyHandler                              CONFIG_VIEW_VISIBLE                       = SH.getKeyHandler("ConfigViewVisible", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.lookandfeel =
    // null
    public static final StringKeyHandler                               LOOK_AND_FEEL                             = SH.getKeyHandler("LookAndFeel", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.lastiffileexists
    // = SKIP_FILE
    public static final EnumKeyHandler                                 LAST_IF_FILE_EXISTS                       = SH.getKeyHandler("LastIfFileExists", EnumKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.captchascalefactor
    // = 100
    /**
     * Captcha Dialog Image scale Faktor in %
     **/
    public static final IntegerKeyHandler                              CAPTCHA_SCALE_FACTOR                      = SH.getKeyHandler("CaptchaScaleFactor", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.selecteddownloadsearchcategory
    // = FILENAME
    public static final EnumKeyHandler                                 SELECTED_DOWNLOAD_SEARCH_CATEGORY         = SH.getKeyHandler("SelectedDownloadSearchCategory", EnumKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.showmovetotopbutton
    // = false
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_TO_TOP_BUTTON                   = SH.getKeyHandler("ShowMoveToTopButton", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.fontname =
    // default
    /**
     * Font to be used. Default value is default.
     **/
    public static final StringKeyHandler                               FONT_NAME                                 = SH.getKeyHandler("FontName", StringKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.linkgrabbersidebarenabled
    // = true
    /**
     * Enable/Disable the Linkgrabber Sidebar
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_ENABLED               = SH.getKeyHandler("LinkgrabberSidebarEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.dialogdefaulttimeout
    // = 20
    public static final IntegerKeyHandler                              DIALOG_DEFAULT_TIMEOUT                    = SH.getKeyHandler("DialogDefaultTimeout", IntegerKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.linkgrabberautotabswitchenabled
    // = true
    /**
     * If enabled, The User Interface will switch to Linkgrabber Tab if a new
     * job has been added
     **/
    public static final BooleanKeyHandler                              LINKGRABBER_AUTO_TAB_SWITCH_ENABLED       = SH.getKeyHandler("LinkgrabberAutoTabSwitchEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.linkgrabbersidebarvisible
    // = true
    public static final BooleanKeyHandler                              LINKGRABBER_SIDEBAR_VISIBLE               = SH.getKeyHandler("LinkgrabberSidebarVisible", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.animationenabled
    // = true
    /**
     * Disable animation and all animation threads. Optional value. Default
     * value is true.
     **/
    public static final BooleanKeyHandler                              ANIMATION_ENABLED                         = SH.getKeyHandler("AnimationEnabled", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.showmoveupbutton
    // = false
    /**
     * True if move button should be visible in downloadview
     **/
    public static final BooleanKeyHandler                              SHOW_MOVE_UP_BUTTON                       = SH.getKeyHandler("ShowMoveUpButton", BooleanKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.settings.GraphicalUserInterfaceSettings.windowopaque =
    // false
    /**
     * Enable/disable window opacity on Java 6u10 and above. A value of 'false'
     * disables window opacity which means that the window corner background
     * which is visible for non-rectangular windows disappear. Furthermore the
     * shadow for popupMenus makes use of real translucent window. Some themes
     * like SyntheticaSimple2D support translucent titlePanes if opacity is
     * disabled. The property is ignored on JRE's below 6u10. Note: It is
     * recommended to activate this feature only if your graphics hardware
     * acceleration is supported by the JVM - a value of 'false' can affect
     * application performance. Default value is false which means the
     * translucency feature is enabled
     **/
    public static final BooleanKeyHandler                              WINDOW_OPAQUE                             = SH.getKeyHandler("WindowOpaque", BooleanKeyHandler.class);
}
