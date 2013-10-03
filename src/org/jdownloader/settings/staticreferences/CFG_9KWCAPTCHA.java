package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.captcha.v2.solver.Captcha9kwSettings;

public class CFG_9KWCAPTCHA {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(Captcha9kwSettings.class);
    }

    // Static Mappings for interface org.jdownloader.captcha.v2.solver.Captcha9kwSettings
    public static final Captcha9kwSettings                 CFG          = JsonConfig.create(Captcha9kwSettings.class);
    public static final StorageHandler<Captcha9kwSettings> SH           = (StorageHandler<Captcha9kwSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // false
    /**
     * Active the 9kw.eu service
     **/
    public static final BooleanKeyHandler                  ENABLED      = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);
    // null
    /**
     * Captcha whitelist for hoster
     **/
    public static final StringKeyHandler                   WHITELIST    = SH.getKeyHandler("whitelist", StringKeyHandler.class);
    // null
    /**
     * Your (User) ApiKey from 9kw.eu
     **/
    public static final StringKeyHandler                   API_KEY      = SH.getKeyHandler("ApiKey", StringKeyHandler.class);
    // true
    /**
     * Activate the Captcha Feedback
     **/
    public static final BooleanKeyHandler                  FEEDBACK     = SH.getKeyHandler("feedback", BooleanKeyHandler.class);
    // 0
    /**
     * Activate the Captcha Feedback
     **/
    public static final BooleanKeyHandler                  SELFSOLVE    = SH.getKeyHandler("Selfsolve", BooleanKeyHandler.class);
    // 0
    /**
     * More priority for captchas (Cost +1-10)
     **/
    public static final IntegerKeyHandler                  PRIO         = SH.getKeyHandler("prio", IntegerKeyHandler.class);
    // 0
    /**
     * Max. Captchas per hour
     **/
    public static final IntegerKeyHandler                  HOUR         = SH.getKeyHandler("hour", IntegerKeyHandler.class);
    // true
    /**
     * Only https requests to 9kw.eu
     **/
    public static final BooleanKeyHandler                  HTTPS        = SH.getKeyHandler("https", BooleanKeyHandler.class);
    // false
    /**
     * Activate the Mouse Captchas
     **/
    public static final BooleanKeyHandler                  MOUSE        = SH.getKeyHandler("mouse", BooleanKeyHandler.class);
    // null
    /**
     * Captcha blacklist for hoster
     **/
    public static final StringKeyHandler                   BLACKLIST    = SH.getKeyHandler("blacklist", StringKeyHandler.class);
    // null
    /**
     * Confirm option for captchas (Cost +6)
     **/
    public static final BooleanKeyHandler                  CONFIRM      = SH.getKeyHandler("confirm", BooleanKeyHandler.class);
    // false
    /**
     * Confirm option for mouse captchas (Cost +6)
     **/
    public static final BooleanKeyHandler                  MOUSECONFIRM = SH.getKeyHandler("mouseconfirm", BooleanKeyHandler.class);
}