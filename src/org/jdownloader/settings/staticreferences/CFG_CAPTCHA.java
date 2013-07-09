package org.jdownloader.settings.staticreferences;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public class CFG_CAPTCHA {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(CaptchaSettings.class);
    }

    // Static Mappings for interface jd.controlling.captcha.CaptchaSettings
    public static final CaptchaSettings                 CFG                                           = JsonConfig.create(CaptchaSettings.class);
    public static final StorageHandler<CaptchaSettings> SH                                            = (StorageHandler<CaptchaSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.
    // true
    /**
     * True to enable a countdown in captcha dialogs. Dialog will close automated after the coundown
     **/
    public static final BooleanKeyHandler               DIALOG_COUNTDOWN_FOR_DOWNLOADS_ENABLED        = SH.getKeyHandler("DialogCountdownForDownloadsEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Disable all Remote Captchas. (Android App, IPhone App, Webinterface, ...)
     **/
    public static final BooleanKeyHandler               REMOTE_CAPTCHA_ENABLED                        = SH.getKeyHandler("RemoteCaptchaEnabled", BooleanKeyHandler.class);
    // true
    /**
     * Disable JAntiCaptcha
     **/
    public static final BooleanKeyHandler               JANTI_CAPTCHA_ENABLED                         = SH.getKeyHandler("JAntiCaptchaEnabled", BooleanKeyHandler.class);
    // 0
    /**
     * Milliseconds after which a Captcha Dialog will appear even if Auto CaptchaBroptherhood Service is still running
     **/
    public static final IntegerKeyHandler               CAPTCHA_DIALOG_CAPTCHA_BROPTHERHOOD_TIMEOUT   = SH.getKeyHandler("CaptchaDialogCaptchaBroptherhoodTimeout", IntegerKeyHandler.class);
    // 20000
    /**
     * MS to wait until captcha dialog gets answered. Close dialog after this timeout unanswered
     **/
    public static final IntegerKeyHandler               DIALOG_COUNTDOWN                              = SH.getKeyHandler("DialogCountdown", IntegerKeyHandler.class);
    // 300000
    /**
     * Timeout after which a challenge (captcha) invalidates (e.g sessions run out...) this timeout should be set by the plugins
     **/
    public static final IntegerKeyHandler               DEFAULT_CHALLENGE_TIMEOUT                     = SH.getKeyHandler("DefaultChallengeTimeout", IntegerKeyHandler.class);
    // true
    /**
     * Disable Captcha Exchangeservices like 9kw or Captchabrotherhood
     **/
    public static final BooleanKeyHandler               CAPTCHA_EXCHANGE_SERVICES_ENABLED             = SH.getKeyHandler("CaptchaExchangeServicesEnabled", BooleanKeyHandler.class);
    // 95
    /**
     * Do not Change me unless you know 100000% what this value is used for!
     **/
    public static final IntegerKeyHandler               AUTO_CAPTCHA_PRIORITY_THRESHOLD               = SH.getKeyHandler("AutoCaptchaPriorityThreshold", IntegerKeyHandler.class);
    // true
    /**
     * Disable, if you do not want JDownloader to autosolve as many captchas as possible
     **/
    public static final BooleanKeyHandler               AUTO_CAPTCHA_RECOGNITION_ENABLED              = SH.getKeyHandler("AutoCaptchaRecognitionEnabled", BooleanKeyHandler.class);
    // false
    /**
     * True to enable a countdown in crawler captcha dialogs. Dialog will close automated after the coundown
     **/
    public static final BooleanKeyHandler               DIALOG_COUNTDOWN_FOR_CRAWLER_ENABLED          = SH.getKeyHandler("DialogCountdownForCrawlerEnabled", BooleanKeyHandler.class);
    // true
    /**
     * If the Dialog Countdown is reached, the link will be skipped. Disable this option to retry instead
     **/
    public static final BooleanKeyHandler               SKIP_DOWNLOAD_LINK_ON_CAPTCHA_TIMEOUT_ENABLED = SH.getKeyHandler("SkipDownloadLinkOnCaptchaTimeoutEnabled", BooleanKeyHandler.class);
    // 300000
    /**
     * Milliseconds after which a Captcha Dialog will appear even if Auto 9kw Service is still running
     **/
    public static final IntegerKeyHandler               CAPTCHA_DIALOG9KW_TIMEOUT                     = SH.getKeyHandler("CaptchaDialog9kwTimeout", IntegerKeyHandler.class);
    // 15000
    /**
     * Milliseconds after which a Captcha Dialog will appear even if Auto AntiCaptcha is still running
     **/
    public static final IntegerKeyHandler               CAPTCHA_DIALOG_JANTI_CAPTCHA_TIMEOUT          = SH.getKeyHandler("CaptchaDialogJAntiCaptchaTimeout", IntegerKeyHandler.class);
    // 120000
    /**
     * Milliseconds after which a Captcha Dialog will appear even if Auto ResolutorCaptcha Service is still running
     **/
    public static final IntegerKeyHandler               CAPTCHA_DIALOG_RESOLUTOR_CAPTCHA_TIMEOUT      = SH.getKeyHandler("CaptchaDialogResolutorCaptchaTimeout", IntegerKeyHandler.class);
    // 90
    /**
     * Do not Change me unless you know 100000% what this value is used for!
     **/
    public static final IntegerKeyHandler               DEFAULT_JACTRUST_THRESHOLD                    = SH.getKeyHandler("DefaultJACTrustThreshold", IntegerKeyHandler.class);
    // true
    /**
     * Disable all Captcha Dialogs.@See SilentMode Settings
     **/
    public static final BooleanKeyHandler               CAPTCHA_DIALOGS_ENABLED                       = SH.getKeyHandler("CaptchaDialogsEnabled", BooleanKeyHandler.class);
}