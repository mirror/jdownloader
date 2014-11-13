package jd.controlling.captcha;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultEnumValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface CaptchaSettings extends ConfigInterface {

    public static enum MODE {
        // AUTO_ONLY,
        NORMAL,
        SKIP_ALL,
        // SKIP_MANUAL
    }

    public static final CaptchaSettings CFG = JsonConfig.create(CaptchaSettings.class);

    @AboutConfig
    @DefaultBooleanValue(true)
    @DescriptionForConfigEntry("Enable the CES & Remote Captcha Bubbles")
    boolean isRemoteCaptchaBubbleEnabled();

    void setRemoteCaptchaBubbleEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("True to enable a countdown in captcha dialogs. Dialog will close automated after the coundown")
    boolean isDialogCountdownForDownloadsEnabled();

    void setDialogCountdownForDownloadsEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("True to enable a countdown in crawler captcha dialogs. Dialog will close automated after the coundown")
    boolean isDialogCountdownForCrawlerEnabled();

    void setDialogCountdownForCrawlerEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(true)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("If the Dialog Countdown is reached, the link will be skipped. Disable this option to retry instead")
    boolean isSkipDownloadLinkOnCaptchaTimeoutEnabled();

    void setSkipDownloadLinkOnCaptchaTimeoutEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(60000)
    @SpinnerValidator(min = 10000, max = 300000)
    @DescriptionForConfigEntry("MS to wait until captcha dialog gets answered. Close dialog after this timeout unanswered")
    int getCaptchaDialogDefaultCountdown();

    void setCaptchaDialogDefaultCountdown(int seconds);

    @AboutConfig
    @DefaultIntValue(95)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Do not Change me unless you know 100000% what this value is used for!")
    int getAutoCaptchaPriorityThreshold();

    void setAutoCaptchaPriorityThreshold(int value);

    @AboutConfig
    @DescriptionForConfigEntry("Captcha Mode")
    @DefaultEnumValue("NORMAL")
    MODE getCaptchaMode();

    void setCaptchaMode(MODE mode);

    @AboutConfig
    @DefaultIntValue(5 * 60 * 1000)
    @DescriptionForConfigEntry("Timeout after which a challenge (captcha) invalidates (e.g sessions run out...) this timeout should be set by the plugins")
    int getDefaultChallengeTimeout();

    void setDefaultChallengeTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(10000)
    @DescriptionForConfigEntry("If the CES Bubble Support is enable, the bubble gives the user a chance to cancel the CES usage. This is the timeout for this skip option")
    int getCaptchaExchangeChanceToSkipBubbleTimeout();

    void setCaptchaExchangeChanceToSkipBubbleTimeout(int ms);

}
