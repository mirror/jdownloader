package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.jdownloader.controlling.packagizer.PackagizerSettings;

public class CFG_PACKAGIZER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(PackagizerSettings.class);
    }

    // Static Mappings for interface
    // org.jdownloader.controlling.packagizer.PackagizerSettings
    public static final PackagizerSettings                 CFG                = JsonConfig.create(PackagizerSettings.class);
    public static final StorageHandler<PackagizerSettings> SH                 = (StorageHandler<PackagizerSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers,
    // access is faster, and we get an error on init if mappings are wrong.
    // Keyhandler interface
    // org.jdownloader.controlling.packagizer.PackagizerSettings.rulelist = []
    public static final ObjectKeyHandler                   RULE_LIST          = SH.getKeyHandler("RuleList", ObjectKeyHandler.class);
    // Keyhandler interface
    // org.jdownloader.controlling.packagizer.PackagizerSettings.packagizerenabled
    // = true
    public static final BooleanKeyHandler                  PACKAGIZER_ENABLED = SH.getKeyHandler("PackagizerEnabled", BooleanKeyHandler.class);
}
