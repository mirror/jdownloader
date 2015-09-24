package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.captcha.v2.solver.endcaptcha.EndCaptchaConfigInterface;

public class CFG_END_CAPTCHA {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(EndCaptchaConfigInterface.class);
    }

    //
    // Static Mappings for interface org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzConfigInterface
    public static final EndCaptchaConfigInterface                 CFG                         = JsonConfig.create(EndCaptchaConfigInterface.class);
    public static final StorageHandler<EndCaptchaConfigInterface> SH                          = (StorageHandler<EndCaptchaConfigInterface>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final BooleanKeyHandler                         ENABLED                     = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    /**
     * Max. Captchas Parallel
     **/
    public static final IntegerKeyHandler                         THREADPOOL_SIZE             = SH.getKeyHandler("ThreadpoolSize", IntegerKeyHandler.class);

    public static final ObjectKeyHandler                          BLACKLIST_ENTRIES           = SH.getKeyHandler("BlacklistEntries", ObjectKeyHandler.class);

    public static final ObjectKeyHandler                          WAIT_FOR_MAP                = SH.getKeyHandler("WaitForMap", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                         BLACK_WHITE_LISTING_ENABLED = SH.getKeyHandler("BlackWhiteListingEnabled", BooleanKeyHandler.class);

    /**
     * Your EndCaptcha.com Password
     **/
    public static final StringKeyHandler                          PASSWORD                    = SH.getKeyHandler("Password", StringKeyHandler.class);

    public static final ObjectKeyHandler                          WHITELIST_ENTRIES           = SH.getKeyHandler("WhitelistEntries", ObjectKeyHandler.class);

    /**
     * Your EndCaptcha.com Username
     **/
    public static final StringKeyHandler                          USER_NAME                   = SH.getKeyHandler("UserName", StringKeyHandler.class);

    /**
     * Activate the Captcha Feedback
     **/
    public static final BooleanKeyHandler                         FEED_BACK_SENDING_ENABLED   = SH.getKeyHandler("FeedBackSendingEnabled", BooleanKeyHandler.class);
}