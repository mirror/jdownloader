package org.jdownloader.extensions.schedulerV2;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_SCHEDULER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(SchedulerConfig.class, "Application.getResource(\"cfg/\" + " + SchedulerExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.schedulerV2.SchedulerConfig
    public static final SchedulerConfig                 CFG               = JsonConfig.create(Application.getResource("cfg/" + SchedulerExtension.class.getName()), SchedulerConfig.class);
    public static final StorageHandler<SchedulerConfig> SH                = (StorageHandler<SchedulerConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final ObjectKeyHandler                ENTRY_LIST        = SH.getKeyHandler("EntryList", ObjectKeyHandler.class);

    /**
     * Blbalblabla is activated
     **/
    public static final BooleanKeyHandler               BLABLABLA_ENABLED = SH.getKeyHandler("BlablablaEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               FRESH_INSTALL     = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               ENABLED           = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler               GUI_ENABLED       = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
}