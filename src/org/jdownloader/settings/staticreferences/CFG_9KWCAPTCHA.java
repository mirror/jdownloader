package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSettings;

public class CFG_9KWCAPTCHA {
    public static void main(String[] args) {

        ConfigUtils.printStaticMappings(Captcha9kwSettings.class);
    }

    // Static Mappings for interface org.jdownloader.captcha.v2.solver.solver9kw.Captcha9kwSettings
    public static final Captcha9kwSettings                 CFG                      = JsonConfig.create(Captcha9kwSettings.class);
    public static final StorageHandler<Captcha9kwSettings> SH                       = (StorageHandler<Captcha9kwSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Activate the Mouse Captchas
     **/
    public static final BooleanKeyHandler                  MOUSE                    = SH.getKeyHandler("mouse", BooleanKeyHandler.class);

    /**
     * Your (User) ApiKey from 9kw.eu
     **/
    public static final StringKeyHandler                   API_KEY                  = SH.getKeyHandler("ApiKey", StringKeyHandler.class);

    /**
     * Activate the Slider Captchas
     **/
    public static final BooleanKeyHandler                  SLIDER                   = SH.getKeyHandler("slider", BooleanKeyHandler.class);

    /**
     * Activate the Captcha Feedback
     **/
    public static final BooleanKeyHandler                  FEEDBACK                 = SH.getKeyHandler("feedback", BooleanKeyHandler.class);

    /**
     * Confirm option for captchas (Cost +6)
     **/
    public static final BooleanKeyHandler                  CONFIRM                  = SH.getKeyHandler("confirm", BooleanKeyHandler.class);

    /**
     * Max. Captchas Parallel
     **/
    public static final IntegerKeyHandler                  THREADPOOL_SIZE          = SH.getKeyHandler("ThreadpoolSize", IntegerKeyHandler.class);

    /**
     * Other max. timeout only for 9kw Service
     **/
    public static final IntegerKeyHandler                  CAPTCHA_OTHER9KW_TIMEOUT = SH.getKeyHandler("CaptchaOther9kwTimeout", IntegerKeyHandler.class);

    /**
     * Hosteroptions for 9kw.eu
     **/
    public static final StringKeyHandler                   HOSTEROPTIONS            = SH.getKeyHandler("hosteroptions", StringKeyHandler.class);

    /**
     * Activate the whitelist
     **/
    public static final BooleanKeyHandler                  WHITELISTCHECK           = SH.getKeyHandler("whitelistcheck", BooleanKeyHandler.class);

    /**
     * Activate the whitelist with timeout
     **/
    public static final BooleanKeyHandler                  WHITELISTTIMEOUTCHECK    = SH.getKeyHandler("whitelisttimeoutcheck", BooleanKeyHandler.class);

    /**
     * Max. Captchas per hour
     **/
    public static final IntegerKeyHandler                  HOUR                     = SH.getKeyHandler("hour", IntegerKeyHandler.class);

    /**
     * Max. Captchas per hour
     **/
    public static final IntegerKeyHandler                  MINUTE                   = SH.getKeyHandler("minute", IntegerKeyHandler.class);

    /**
     * Activate the blacklist with prio
     **/
    public static final BooleanKeyHandler                  BLACKLISTPRIOCHECK       = SH.getKeyHandler("blacklistpriocheck", BooleanKeyHandler.class);

    /**
     * Captcha blacklist for hoster with prio
     **/
    public static final StringKeyHandler                   BLACKLISTPRIO            = SH.getKeyHandler("blacklistprio", StringKeyHandler.class);

    /**
     * Captcha blacklist for hoster
     **/
    public static final StringKeyHandler                   BLACKLIST                = SH.getKeyHandler("blacklist", StringKeyHandler.class);

    /**
     * Activate the option selfsolve
     **/
    public static final BooleanKeyHandler                  SELFSOLVE                = SH.getKeyHandler("Selfsolve", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                  ENABLED                  = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    /**
     * Activate the lowcredits dialog
     **/
    public static final BooleanKeyHandler                  LOWCREDITS               = SH.getKeyHandler("lowcredits", BooleanKeyHandler.class);

    /**
     * Activate the Puzzle Captchas
     **/
    public static final BooleanKeyHandler                  PUZZLE                   = SH.getKeyHandler("puzzle", BooleanKeyHandler.class);

    /**
     * Captcha whitelist for hoster with timeout
     **/
    public static final StringKeyHandler                   WHITELISTTIMEOUT         = SH.getKeyHandler("whitelisttimeout", StringKeyHandler.class);

    /**
     * Only https requests to 9kw.eu
     **/
    public static final BooleanKeyHandler                  HTTPS                    = SH.getKeyHandler("https", BooleanKeyHandler.class);

    /**
     * Captcha whitelist for hoster
     **/
    public static final StringKeyHandler                   WHITELIST                = SH.getKeyHandler("whitelist", StringKeyHandler.class);

    /**
     * Activate the whitelist with prio
     **/
    public static final BooleanKeyHandler                  WHITELISTPRIOCHECK       = SH.getKeyHandler("whitelistpriocheck", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                   WAIT_FOR_MAP             = SH.getKeyHandler("WaitForMap", ObjectKeyHandler.class);

    /**
     * Activate the blacklist with timeout
     **/
    public static final BooleanKeyHandler                  BLACKLISTTIMEOUTCHECK    = SH.getKeyHandler("blacklisttimeoutcheck", BooleanKeyHandler.class);

    /**
     * Default Timeout in ms
     **/
    public static final IntegerKeyHandler                  DEFAULT_TIMEOUT          = SH.getKeyHandler("DefaultTimeout", IntegerKeyHandler.class);

    /**
     * More priority for captchas (Cost +1-20)
     **/
    public static final IntegerKeyHandler                  PRIO                     = SH.getKeyHandler("prio", IntegerKeyHandler.class);

    public static final BooleanKeyHandler                  ENABLED_GLOBALLY         = SH.getKeyHandler("EnabledGlobally", BooleanKeyHandler.class);

    /**
     * Captcha whitelist for hoster with prio
     **/
    public static final StringKeyHandler                   WHITELISTPRIO            = SH.getKeyHandler("whitelistprio", StringKeyHandler.class);

    /**
     * Captcha blacklist for hoster with timeout
     **/
    public static final StringKeyHandler                   BLACKLISTTIMEOUT         = SH.getKeyHandler("blacklisttimeout", StringKeyHandler.class);

    /**
     * Activate the blacklist
     **/
    public static final BooleanKeyHandler                  BLACKLISTCHECK           = SH.getKeyHandler("blacklistcheck", BooleanKeyHandler.class);

    /**
     * Activate the debugmode for 9kw.eu service
     **/
    public static final BooleanKeyHandler                  DEBUG                    = SH.getKeyHandler("Debug", BooleanKeyHandler.class);

    /**
     * Confirm option for mouse captchas (Cost +6)
     **/
    public static final BooleanKeyHandler                  MOUSECONFIRM             = SH.getKeyHandler("mouseconfirm", BooleanKeyHandler.class);
}