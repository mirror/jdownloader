package org.jdownloader.settings.staticreferences;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzConfigInterface;

public class CFG_IMAGE_TYPERZ {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(ImageTyperzConfigInterface.class);
    }

    // Static Mappings for interface org.jdownloader.captcha.v2.solver.imagetyperz.ImageTyperzConfigInterface
    public static final ImageTyperzConfigInterface                 CFG                       = JsonConfig.create(ImageTyperzConfigInterface.class);
    public static final StorageHandler<ImageTyperzConfigInterface> SH                        = (StorageHandler<ImageTyperzConfigInterface>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Activate the Captcha Feedback
     **/
    public static final BooleanKeyHandler                          FEED_BACK_SENDING_ENABLED = SH.getKeyHandler("FeedBackSendingEnabled", BooleanKeyHandler.class);

    /**
     * Captcha WhiteList for hoster
     **/
    public static final StringKeyHandler                           WHITE_LIST                = SH.getKeyHandler("WhiteList", StringKeyHandler.class);

    /**
     * Your ImageTyperz.com Password
     **/
    public static final StringKeyHandler                           PASSWORD                  = SH.getKeyHandler("Password", StringKeyHandler.class);

    /**
     * Captcha BlackList for hoster
     **/
    public static final StringKeyHandler                           BLACK_LIST                = SH.getKeyHandler("BlackList", StringKeyHandler.class);

    public static final BooleanKeyHandler                          ENABLED                   = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                           WAIT_FOR_MAP              = SH.getKeyHandler("WaitForMap", ObjectKeyHandler.class);

    /**
     * Max. Captchas Parallel
     **/
    public static final IntegerKeyHandler                          THREADPOOL_SIZE           = SH.getKeyHandler("ThreadpoolSize", IntegerKeyHandler.class);

    /**
     * Your ImageTyperz.com Username
     **/
    public static final StringKeyHandler                           USER_NAME                 = SH.getKeyHandler("UserName", StringKeyHandler.class);
}