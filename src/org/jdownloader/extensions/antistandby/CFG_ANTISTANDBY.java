package org.jdownloader.extensions.antistandby;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_ANTISTANDBY {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(AntiStandbyConfig.class);
    }

    // Static Mappings for interface
    // org.jdownloader.extensions.shutdown.AntiStandbyConfig
    public static final AntiStandbyConfig                 CFG           = JsonConfig.create(Application.getResource("cfg/" + AntiStandbyExtension.class.getName()), AntiStandbyConfig.class);
    public static final StorageHandler<AntiStandbyConfig> SH            = (StorageHandler<AntiStandbyConfig>) CFG.getStorageHandler();
    // true
    public static final BooleanKeyHandler                 FRESH_INSTALL = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                 GUI_ENABLED   = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                 ENABLED       = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // DOWNLOADING
    public static final EnumKeyHandler                    MODE          = SH.getKeyHandler("Mode", EnumKeyHandler.class);
}