package jd.controlling.captcha;

import java.util.ArrayList;
import java.util.HashMap;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.LookUpKeys;
import org.appwork.storage.config.annotations.SpinnerValidator;
import org.jdownloader.captcha.v2.CaptchaQualityEnsuranceRule;

public interface CaptchaSettings extends ConfigInterface {
    public static enum MODE {
        // AUTO_ONLY,
        NORMAL,
        SKIP_ALL,
        // SKIP_MANUAL
    }

    public static enum CAPTCHA_TIMEOUT_ACTION {
        SKIP,
        SKIP_HOSTER,
        RETRY,
        ASK
    }

    public static final CaptchaSettings CFG = JsonConfig.create(CaptchaSettings.class);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable the CES & Remote Captcha Bubbles")
    boolean isRemoteCaptchaBubbleEnabled();

    void setRemoteCaptchaBubbleEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Enable the Captcha (e.g. CES) for Accountlogins. Use at your own risk!")
    boolean isCaptchaExchangeForAccountLoginEnabled();

    void setCaptchaExchangeForAccountLoginEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("True to enable a countdown in captcha dialogs. Dialog will close automated after the coundown")
    boolean isDialogCountdownForDownloadsEnabled();

    void setDialogCountdownForDownloadsEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("True to enable a countdown in crawler captcha dialogs. Dialog will close automated after the coundown")
    boolean isDialogCountdownForCrawlerEnabled();

    void setDialogCountdownForCrawlerEnabled(boolean b);

    @AboutConfig
    @DefaultEnumValue("SKIP")
    @LookUpKeys({ "captchatimeoutaction", "hostercaptchatimeoutaction" })
    @DescriptionForConfigEntry("Defines what should happen if a captcha prompt runs into a timeout meaning that neither the user nor any captcha solver did answer the captcha prompt. This setting affects all HOSTER download/account-check processes.")
    CAPTCHA_TIMEOUT_ACTION getOnHosterCaptchaTimeoutAction();

    void setOnHosterCaptchaTimeoutAction(CAPTCHA_TIMEOUT_ACTION b);

    @AboutConfig
    @DefaultEnumValue("SKIP")
    @LookUpKeys("crawlercaptchatimeoutaction")
    @DescriptionForConfigEntry("Defines what should happen if a captcha prompt runs into a timeout meaning that neither the user nor any captcha solver did answer the captcha prompt. This setting affects all CRAWLER processes.")
    CAPTCHA_TIMEOUT_ACTION getOnCrawlerCaptchaTimeoutAction();

    void setOnCrawlerCaptchaTimeoutAction(CAPTCHA_TIMEOUT_ACTION b);

    @AboutConfig
    @DefaultIntValue(300000)
    @SpinnerValidator(min = 10000, max = 900000)
    @DescriptionForConfigEntry("MS to wait until captcha dialog gets answered. Close dialog after this timeout unanswered")
    int getCaptchaDialogDefaultCountdown2();

    void setCaptchaDialogDefaultCountdown2(int seconds);

    @AboutConfig
    @DefaultIntValue(95)
    @DescriptionForConfigEntry("Do not Change me unless you know 100000% what this value is used for!")
    int getAutoCaptchaPriorityThreshold();

    void setAutoCaptchaPriorityThreshold(int value);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha Mode")
    @DefaultEnumValue("NORMAL")
    MODE getCaptchaMode();

    void setCaptchaMode(MODE mode);

    @AboutConfig
    @DefaultIntValue(10 * 60 * 1000)
    @DescriptionForConfigEntry("Timeout in milliseconds after which a challenge (captcha) invalidates (e.g sessions run out...) this timeout should be set by the plugins")
    int getDefaultChallengeTimeout();

    void setDefaultChallengeTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(10000)
    @DescriptionForConfigEntry("If the CES Bubble Support is enable, the bubble gives the user a chance to cancel the CES usage. This is the timeout for this skip option")
    int getCaptchaExchangeChanceToSkipBubbleTimeout();

    void setCaptchaExchangeChanceToSkipBubbleTimeout(int ms);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isCancelDialogCountdownOnHateCaptchaClick();

    void setCancelDialogCountdownOnHateCaptchaClick(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isCancelDialogCountdownOnMouseMove();

    void setCancelDialogCountdownOnMouseMove(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    boolean isCancelDialogCountdownOnMouseClick();

    void setCancelDialogCountdownOnMouseClick(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("If you change these rules, Captchas might become very hard or even impossible to solve over time. Do NEVER ever change these rules without knowing what you are doing")
    HashMap<String, ArrayList<CaptchaQualityEnsuranceRule>> getQualityEnsuranceRules();

    void setQualityEnsuranceRules(HashMap<String, ArrayList<CaptchaQualityEnsuranceRule>> map);
}
