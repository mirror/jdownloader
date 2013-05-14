package org.jdownloader.extensions.translator;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.utils.Application;

public class CFG_TRANSLATOR {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(TranslatorConfig.class, "Application.getResource(\"cfg/\" + " + TranslatorExtension.class.getSimpleName() + ".class.getName())");
    }

    // Static Mappings for interface org.jdownloader.extensions.translator.TranslatorConfig
    public static final TranslatorConfig                 CFG                     = JsonConfig.create(Application.getResource("cfg/" + TranslatorExtension.class.getName()), TranslatorConfig.class);
    public static final StorageHandler<TranslatorConfig> SH                      = (StorageHandler<TranslatorConfig>) CFG.getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    public static final BooleanKeyHandler                FRESH_INSTALL           = SH.getKeyHandler("FreshInstall", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                REMEMBER_LOGINS_ENABLED = SH.getKeyHandler("RememberLoginsEnabled", BooleanKeyHandler.class);
    // false
    public static final BooleanKeyHandler                GUI_ENABLED             = SH.getKeyHandler("GuiEnabled", BooleanKeyHandler.class);
    // null
    /**
     * Password for the SVN Access
     **/
    public static final StringKeyHandler                 SVNPASSWORD             = SH.getKeyHandler("SVNPassword", StringKeyHandler.class);
    // 200
    public static final IntegerKeyHandler                QUICK_EDIT_HEIGHT       = SH.getKeyHandler("QuickEditHeight", IntegerKeyHandler.class);
    // false
    public static final BooleanKeyHandler                ENABLED                 = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // null
    public static final StringKeyHandler                 LAST_LOADED             = SH.getKeyHandler("LastLoaded", StringKeyHandler.class);
    // null
    /**
     * Username for the SVN Access
     **/
    public static final StringKeyHandler                 SVNUSER                 = SH.getKeyHandler("SVNUser", StringKeyHandler.class);
    // true
    public static final BooleanKeyHandler                QUICK_EDIT_BAR_VISIBLE  = SH.getKeyHandler("QuickEditBarVisible", BooleanKeyHandler.class);
}