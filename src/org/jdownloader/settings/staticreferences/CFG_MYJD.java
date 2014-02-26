package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ListHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.api.myjdownloader.MyJDownloaderSettings;

public class CFG_MYJD {
    
    // Static Mappings for interface org.jdownloader.api.myjdownloader.MyJDownloaderSettings
    public static final MyJDownloaderSettings                 CFG                    = JsonConfig.create(MyJDownloaderSettings.class);
    public static final StorageHandler<MyJDownloaderSettings> SH                     = (StorageHandler<MyJDownloaderSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // NONE
    public static final EnumKeyHandler                        LATEST_ERROR           = SH.getKeyHandler("LatestError", EnumKeyHandler.class);
    
    // null
    public static final StringKeyHandler                      UNIQUE_DEVICE_ID       = SH.getKeyHandler("UniqueDeviceID", StringKeyHandler.class);
    // api.jdownloader.org
    public static final StringKeyHandler                      CONNECT_IP             = SH.getKeyHandler("ConnectIP", StringKeyHandler.class);
    // true
    public static final BooleanKeyHandler                     AUTO_CONNECT_ENABLED   = SH.getKeyHandler("AutoConnectEnabledV2", BooleanKeyHandler.class);
    // true
    public static final BooleanKeyHandler                     DIRECT_CONNECT_ENABLED = SH.getKeyHandler("DirectConnectEnabled", BooleanKeyHandler.class);
    // null
    public static final StringKeyHandler                      PASSWORD               = SH.getKeyHandler("Password", StringKeyHandler.class);
    // 80
    public static final IntegerKeyHandler                     CLIENT_CONNECT_PORT    = SH.getKeyHandler("ClientConnectPort", IntegerKeyHandler.class);
    // [I@7df36af6
    public static final ListHandler<int[]>                    DEVICE_CONNECT_PORTS   = (ListHandler<int[]>) SH.getKeyHandler("DeviceConnectPorts", ListHandler.class);
    // JDownloader
    public static final StringKeyHandler                      DEVICE_NAME            = SH.getKeyHandler("DeviceName", StringKeyHandler.class);
    // null
    public static final StringKeyHandler                      EMAIL                  = SH.getKeyHandler("Email", StringKeyHandler.class);
}