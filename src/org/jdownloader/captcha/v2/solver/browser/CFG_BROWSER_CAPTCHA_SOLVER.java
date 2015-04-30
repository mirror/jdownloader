package org.jdownloader.captcha.v2.solver.browser;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.ObjectKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;
import org.appwork.storage.config.handler.StringKeyHandler;
import org.appwork.storage.config.handler.StringListHandler;

public class CFG_BROWSER_CAPTCHA_SOLVER {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(BrowserCaptchaSolverConfig.class);
    }

    // Static Mappings for interface org.jdownloader.captcha.v2.solver.browser.BrowserCaptchaSolverConfig
    public static final BrowserCaptchaSolverConfig                 CFG                                     = JsonConfig.create(BrowserCaptchaSolverConfig.class);
    public static final StorageHandler<BrowserCaptchaSolverConfig> SH                                      = (StorageHandler<BrowserCaptchaSolverConfig>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    public static final IntegerKeyHandler                          LOCAL_HTTP_PORT                         = SH.getKeyHandler("LocalHttpPort", IntegerKeyHandler.class);

    public static final ObjectKeyHandler                           BLACKLIST_ENTRIES                       = SH.getKeyHandler("BlacklistEntries", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                          AUTO_OPEN_BROWSER_ENABLED               = SH.getKeyHandler("AutoOpenBrowserEnabled", BooleanKeyHandler.class);

    public static final ObjectKeyHandler                           WAIT_FOR_MAP                            = SH.getKeyHandler("WaitForMap", ObjectKeyHandler.class);

    public static final BooleanKeyHandler                          BROWSER_LOOP_USER_CONFIRMED             = SH.getKeyHandler("BrowserLoopUserConfirmed", BooleanKeyHandler.class);

    /**
     * Example: [ "C:\\Program Files (x86)\\Google\\Chrome\\Application\\chrome.exe", "-app=%s" ]
     **/
    public static final StringListHandler                          BROWSER_COMMANDLINE                     = SH.getKeyHandler("BrowserCommandline", StringListHandler.class);

    /**
     * If enabled, JD will use your default browser to improve the captcha detection.
     **/
    public static final BooleanKeyHandler                          BROWSER_LOOP_ENABLED                    = SH.getKeyHandler("BrowserLoopEnabled", BooleanKeyHandler.class);

    /**
     * If enabled, JD will use the browserloop even if it is in silentmode
     **/
    public static final BooleanKeyHandler                          BROWSER_LOOP_DURING_SILENT_MODE_ENABLED = SH.getKeyHandler("BrowserLoopDuringSilentModeEnabled", BooleanKeyHandler.class);

    /**
     * Easier Captchas: Get your current google.com SID and HSID Cookie value from your default browser and enter id here.
     **/
    public static final StringKeyHandler                           GOOGLE_COM_COOKIE_VALUE_HSID            = SH.getKeyHandler("GoogleComCookieValueHSID", StringKeyHandler.class);

    public static final ObjectKeyHandler                           WHITELIST_ENTRIES                       = SH.getKeyHandler("WhitelistEntries", ObjectKeyHandler.class);

    /**
     * Easier Captchas: Get your current google.com SID and HSID Cookie value from your default browser and enter id here.
     **/
    public static final StringKeyHandler                           GOOGLE_COM_COOKIE_VALUE_SID             = SH.getKeyHandler("GoogleComCookieValueSID", StringKeyHandler.class);

    public static final BooleanKeyHandler                          ENABLED                                 = SH.getKeyHandler("Enabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                          AUTO_CLICK_ENABLED                      = SH.getKeyHandler("AutoClickEnabled", BooleanKeyHandler.class);

    public static final BooleanKeyHandler                          BLACK_WHITE_LISTING_ENABLED             = SH.getKeyHandler("BlackWhiteListingEnabled", BooleanKeyHandler.class);
}