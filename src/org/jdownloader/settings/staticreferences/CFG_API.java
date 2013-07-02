package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.api.RemoteAPIConfig;

public class CFG_API {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(RemoteAPIConfig.class);
    }

    // Static Mappings for interface org.jdownloader.api.RemoteAPIConfig
    public static final RemoteAPIConfig                 CFG                      = JsonConfig.create(RemoteAPIConfig.class);
    public static final StorageHandler<RemoteAPIConfig> SH                       = (StorageHandler<RemoteAPIConfig>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    /**
     * ExternInterface(Cnl2,Flashgot) will listen on 9666
     **/
    public static final BooleanKeyHandler               EXTERN_INTERFACE_ENABLED = SH.getKeyHandler("ExternInterfaceEnabled", BooleanKeyHandler.class);
    // null
    /**
     * ExternInterface(Cnl2,Flashgot) Authorized Websites
     **/
    public static final ObjectKeyHandler                EXTERN_INTERFACE_AUTH    = SH.getKeyHandler("ExternInterfaceAuth", ObjectKeyHandler.class);
    // false
    /**
     * Enable or disable the JDAnywhere API
     **/
    public static final BooleanKeyHandler               JDANYWHERE_API_ENABLED   = SH.getKeyHandler("JDAnywhereApiEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler               DEPRECATED_API_ENABLED   = SH.getKeyHandler("DeprecatedApiEnabled", BooleanKeyHandler.class);
}