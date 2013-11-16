package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSettings;

public class CFG_DBC {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(DeathByCaptchaSettings.class);
    }

    // Static Mappings for interface org.jdownloader.captcha.v2.solver.dbc.DeathByCaptchaSettings
    public static final DeathByCaptchaSettings                 CFG                       = JsonConfig.create(DeathByCaptchaSettings.class);
    public static final StorageHandler<DeathByCaptchaSettings> SH                        = (StorageHandler<DeathByCaptchaSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Captcha BlackList for hoster
     **/
    public static final StringKeyHandler                       BLACK_LIST                = SH.getKeyHandler("BlackList", StringKeyHandler.class);

    /**
     * Max. Captchas Parallel
     **/
    public static final IntegerKeyHandler                      THREADPOOL_SIZE           = SH.getKeyHandler("ThreadpoolSize", IntegerKeyHandler.class);

    /**
     * Captcha WhiteList for hoster
     **/
    public static final StringKeyHandler                       WHITE_LIST                = SH.getKeyHandler("WhiteList", StringKeyHandler.class);

    /**
     * Active the deathbycaptcha.eu service
     **/
    public static final BooleanKeyHandler                      ENABLED                   = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    /**
     * Activate the Captcha Feedback
     **/
    public static final BooleanKeyHandler                      FEED_BACK_SENDING_ENABLED = SH.getKeyHandler("FeedBackSendingEnabled", BooleanKeyHandler.class);

    /**
     * Your deathbycaptcha.eu Username
     **/
    public static final StringKeyHandler                       USER_NAME                 = SH.getKeyHandler("UserName", StringKeyHandler.class);

    /**
     * Your deathbycaptcha.eu Password
     **/
    public static final StringKeyHandler                       PASSWORD                  = SH.getKeyHandler("Password", StringKeyHandler.class);
}