package org.jdownloader.gui.jdtrayicon;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_TRAY_CONFIG {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(TrayConfig.class, "Application.getResource(\"cfg/\" + " + TrayExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.gui.jdtrayicon.TrayConfig
    public static final TrayConfig                 CFG                                              = JsonConfig.create(Application.getResource("cfg/" + TrayExtension.class.getName()), TrayConfig.class);
    public static final StorageHandler<TrayConfig> SH                                               = (StorageHandler<TrayConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler          TOOL_TIP_ENABLED                                 = SH.getKeyHandler("ToolTipEnabled", BooleanKeyHandler.class);
    // ASK
    public static final EnumKeyHandler             ON_CLOSE_ACTION                                  = SH.getKeyHandler("OnCloseAction", EnumKeyHandler.class);
    // true
    public static final BooleanKeyHandler          GNOME_TRAY_ICON_TRANSPARENT_ENABLED              = SH.getKeyHandler("GnomeTrayIconTransparentEnabled", BooleanKeyHandler.class);
    // TO_TASKBAR
    public static final EnumKeyHandler             ON_MINIMIZE_ACTION                               = SH.getKeyHandler("OnMinimizeAction", EnumKeyHandler.class);
    // false
    public static final BooleanKeyHandler          GREY_ICON_ENABLED                                = SH.getKeyHandler("GreyIconEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          TRAY_ONLY_VISIBLE_IF_WINDOW_IS_HIDDEN_ENABLED    = SH.getKeyHandler("TrayOnlyVisibleIfWindowIsHiddenEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          GUI_ENABLED                                      = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          TOOGLE_WINDOW_STATUS_WITH_SINGLE_CLICK_ENABLED   = SH.getKeyHandler("ToogleWindowStatusWithSingleClickEnabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler          FRESH_INSTALL                                    = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          START_MINIMIZED_ENABLED                          = SH.getKeyHandler("StartMinimizedEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          BALLON_NOTIFY_ON_NEW_LINKGRABBER_LINKS_ENABLED   = SH.getKeyHandler("BallonNotifyOnNewLinkgrabberLinksEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          BALLON_NOTIFY_ON_NEW_LINKGRABBER_PACKAGE_ENABLED = SH.getKeyHandler("BallonNotifyOnNewLinkgrabberPackageEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          ENABLED                                          = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
}