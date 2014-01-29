package org.jdownloader.extensions.folderwatch;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.LongKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringListHandler;
import org.appwork.utils.Application;

public class CFG_FOLDER_WATCH {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(FolderWatchConfig.class, "Application.getResource(\"cfg/\" + " + FolderWatchExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.folderwatch.FolderWatchConfig
    public static final FolderWatchConfig                 CFG            = JsonConfig.create(Application.getResource("cfg/" + FolderWatchExtension.class.getName()), FolderWatchConfig.class);
    public static final StorageHandler<FolderWatchConfig> SH             = (StorageHandler<FolderWatchConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final BooleanKeyHandler                 FRESH_INSTALL  = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                 GUI_ENABLED    = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);

    public static final LongKeyHandler                    CHECK_INTERVAL = SH.getKeyHandler("CheckInterval", LongKeyHandler.class);

    public static final StringListHandler                 FOLDERS        = SH.getKeyHandler("Folders", StringListHandler.class);

    public static final BooleanKeyHandler                 ENABLED        = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
}