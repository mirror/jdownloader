package org.jdownloader.extensions.schedulerV2;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_SCHEDULER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(SchedulerConfig.class, "Application.getResource(\"cfg/\" + " + SchedulerExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.folderwatch.FolderWatchConfig
    public static final SchedulerConfig                 CFG = JsonConfig.create(Application.getResource("cfg/" + SchedulerExtension.class.getName()), SchedulerConfig.class);
    public static final StorageHandler<SchedulerConfig> SH  = (StorageHandler<SchedulerConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

}