package org.jdownloader.extensions.infobar;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_INFOBAR {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(InfoBarConfig.class, "Application.getResource(\"cfg/\" + " + InfoBarExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.infobar.InfoBarConfig
    public static final InfoBarConfig                 CFG           = JsonConfig.create(Application.getResource("cfg/" + InfoBarExtension.class.getName()), InfoBarConfig.class);
    public static final StorageHandler<InfoBarConfig> SH            = (StorageHandler<InfoBarConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler             FRESH_INSTALL = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler             GUI_ENABLED   = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler             ENABLED       = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
}