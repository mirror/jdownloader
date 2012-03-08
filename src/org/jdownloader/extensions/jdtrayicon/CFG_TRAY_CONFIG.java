package org.jdownloader.extensions.jdtrayicon;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.Application;

public class CFG_TRAY_CONFIG {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(TrayConfig.class);
    }

    // Static Mappings for interface
    // org.jdownloader.extensions.jdtrayicon.TrayConfig
    public static final TrayConfig                 CFG                                            = JsonConfig.create(Application.getResource("cfg/" + TrayExtension.class.getName()), TrayConfig.class);
    public static final StorageHandler<TrayConfig> SH                                             = (StorageHandler<TrayConfig>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // true
    public static final BooleanKeyHandler          FRESH_INSTALL                                  = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          GUI_ENABLED                                    = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // ALWAYS
    public static final EnumKeyHandler             SHOW_LINKGRABBING_RESULTS_OPTION               = SH.getKeyHandler("ShowLinkgrabbingResultsOption", EnumKeyHandler.class);
    // false
    public static final BooleanKeyHandler          TOOGLE_WINDOW_STATUS_WITH_SINGLE_CLICK_ENABLED = SH.getKeyHandler("ToogleWindowStatusWithSingleClickEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          PASSWORD_PROTECTION_ENABLED                    = SH.getKeyHandler("PasswordProtectionEnabled", BooleanKeyHandler.class);
    // null
    public static final StringKeyHandler           PASSWORD                                       = SH.getKeyHandler("Password", StringKeyHandler.class);
    // false
    public static final BooleanKeyHandler          ENABLED                                        = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler          CLOSE_TO_TRAY_ENABLED                          = SH.getKeyHandler("CloseToTrayEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          TOOL_TIP_ENABLED                               = SH.getKeyHandler("ToolTipEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler          START_MINIMIZED_ENABLED                        = SH.getKeyHandler("StartMinimizedEnabled", BooleanKeyHandler.class);
}