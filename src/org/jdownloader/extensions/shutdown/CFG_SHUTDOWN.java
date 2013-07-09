package org.jdownloader.extensions.shutdown;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.utils.Application;

public class CFG_SHUTDOWN {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(ShutdownConfig.class, "Application.getResource(\"cfg/\" + " + ShutdownExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.shutdown.ShutdownConfig
    public static final ShutdownConfig                 CFG                                = JsonConfig.create(Application.getResource("cfg/" + ShutdownExtension.class.getName()), ShutdownConfig.class);
    public static final StorageHandler<ShutdownConfig> SH                                 = (StorageHandler<ShutdownConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler              FRESH_INSTALL                      = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler              GUI_ENABLED                        = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // 60
    public static final IntegerKeyHandler              COUNTDOWN_TIME                     = SH.getKeyHandler("CountdownTime", IntegerKeyHandler.class);
    // SHUTDOWN
    public static final EnumKeyHandler                 SHUTDOWN_MODE                      = SH.getKeyHandler("ShutdownMode", EnumKeyHandler.class);
    // true
    /**
     * If you want the 'Shutdown enabled' flag to be disabled in a new session, then disable this flag
     **/
    public static final BooleanKeyHandler              SHUTDOWN_ACTIVE_BY_DEFAULT_ENABLED = SH.getKeyHandler("ShutdownActiveByDefaultEnabled", BooleanKeyHandler.class);
    // false
    /**
     * If enabled, JD will shut down the system after downloads have finished
     **/
    public static final BooleanKeyHandler              SHUTDOWN_ACTIVE                    = SH.getKeyHandler("ShutdownActive", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler              ENABLED                            = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // false
    /**
     * Forcing Shutdown works only on some systems.
     **/
    public static final BooleanKeyHandler              FORCE_SHUTDOWN_ENABLED             = SH.getKeyHandler("ForceShutdownEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler              FORCE_FOR_MAC_INSTALLED            = SH.getKeyHandler("ForceForMacInstalled", BooleanKeyHandler.class);
}