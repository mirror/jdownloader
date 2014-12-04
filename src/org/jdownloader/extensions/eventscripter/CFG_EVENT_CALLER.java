package org.jdownloader.extensions.eventscripter;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_EVENT_CALLER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(EventScripterConfig.class, "Application.getResource(\"cfg/\" + " + EventScripterExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.schedulerV2.SchedulerConfig
    public static final EventScripterConfig                 CFG           = JsonConfig.create(Application.getResource("cfg/" + EventScripterExtension.class.getName()), EventScripterConfig.class);
    public static final StorageHandler<EventScripterConfig> SH            = (StorageHandler<EventScripterConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final ObjectKeyHandler                  SCRIPTS       = SH.getKeyHandler("Scripts", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                 FRESH_INSTALL = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                 ENABLED       = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                 GUI_ENABLED   = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
}