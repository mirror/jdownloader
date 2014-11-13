package org.jdownloader.settings.staticreferences;

import jd.controlling.captcha.CaptchaSettings;

import org.appwork.storage.config.ConfigUtils;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.handler.BooleanKeyHandler;
import org.appwork.storage.config.handler.EnumKeyHandler;
import org.appwork.storage.config.handler.IntegerKeyHandler;
import org.appwork.storage.config.handler.StorageHandler;

public class CFG_CAPTCHA {
    public static void main(String[] args) {
        ConfigUtils.printStaticMappings(CaptchaSettings.class);
    }

    // Static Mappings for interface jd.controlling.captcha.CaptchaSettings
    public static final CaptchaSettings                 CFG                                            = JsonConfig.create(CaptchaSettings.class);
    public static final StorageHandler<CaptchaSettings> SH                                             = (StorageHandler<CaptchaSettings>) CFG._getStorageHandler();
    // let's do this mapping here. If we map all methods to static handlers, access is faster, and we get an error on init if mappings are
    // wrong.

    /**
     * Do not Change me unless you know 100000% what this value is used for!
     **/
    public static final IntegerKeyHandler               AUTO_CAPTCHA_PRIORITY_THRESHOLD                = SH.getKeyHandler("AutoCaptchaPriorityThreshold", IntegerKeyHandler.class);

    /**
     * Timeout after which a challenge (captcha) invalidates (e.g sessions run out...) this timeout should be set by the plugins
     **/
    public static final IntegerKeyHandler               DEFAULT_CHALLENGE_TIMEOUT                      = SH.getKeyHandler("DefaultChallengeTimeout", IntegerKeyHandler.class);

    /**
     * True to enable a countdown in captcha dialogs. Dialog will close automated after the coundown
     **/
    public static final BooleanKeyHandler               DIALOG_COUNTDOWN_FOR_DOWNLOADS_ENABLED         = SH.getKeyHandler("DialogCountdownForDownloadsEnabled", BooleanKeyHandler.class);

    /**
     * If the CES Bubble Support is enable, the bubble gives the user a chance to cancel the CES usage. This is the timeout for this skip
     * option
     **/
    public static final IntegerKeyHandler               CAPTCHA_EXCHANGE_CHANCE_TO_SKIP_BUBBLE_TIMEOUT = SH.getKeyHandler("CaptchaExchangeChanceToSkipBubbleTimeout", IntegerKeyHandler.class);

    /**
     * True to enable a countdown in crawler captcha dialogs. Dialog will close automated after the coundown
     **/
    public static final BooleanKeyHandler               DIALOG_COUNTDOWN_FOR_CRAWLER_ENABLED           = SH.getKeyHandler("DialogCountdownForCrawlerEnabled", BooleanKeyHandler.class);

    /**
     * If the Dialog Countdown is reached, the link will be skipped. Disable this option to retry instead
     **/
    public static final BooleanKeyHandler               SKIP_DOWNLOAD_LINK_ON_CAPTCHA_TIMEOUT_ENABLED  = SH.getKeyHandler("SkipDownloadLinkOnCaptchaTimeoutEnabled", BooleanKeyHandler.class);

    /**
     * Enable the CES & Remote Captcha Bubbles
     **/
    public static final BooleanKeyHandler               REMOTE_CAPTCHA_BUBBLE_ENABLED                  = SH.getKeyHandler("RemoteCaptchaBubbleEnabled", BooleanKeyHandler.class);

    /**
     * Captcha Mode
     **/
    public static final EnumKeyHandler                  CAPTCHA_MODE                                   = SH.getKeyHandler("CaptchaMode", EnumKeyHandler.class);

    /**
     * MS to wait until captcha dialog gets answered. Close dialog after this timeout unanswered
     **/
    public static final IntegerKeyHandler               CAPTCHA_DIALOG_DEFAULT_COUNTDOWN               = SH.getKeyHandler("CaptchaDialogDefaultCountdown", IntegerKeyHandler.class);
}