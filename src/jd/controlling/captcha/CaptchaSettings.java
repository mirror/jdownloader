package jd.controlling.captcha;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.JsonConfig;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface CaptchaSettings extends ConfigInterface {

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
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
    @DescriptionForConfigEntry("MS to wait until captcha dialog gets answered. Close dialog after this timeout unanswered")
    int getCaptchaDialogDefaultCountdown();

    void setCaptchaDialogDefaultCountdown(int seconds);

    // @DefaultIntValue(0)
    // int getLastCancelOption();
    //
    // void setLastCancelOption(int i);

    @AboutConfig
    @DefaultBooleanValue(true)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Disable, if you do not want JDownloader to autosolve as many captchas as possible")
    boolean isAutoCaptchaRecognitionEnabled();

    void setAutoCaptchaRecognitionEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(95)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Do not Change me unless you know 100000% what this value is used for!")
    int getAutoCaptchaPriorityThreshold();

    void setAutoCaptchaPriorityThreshold(int value);

    @AboutConfig
    @DefaultIntValue(90)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Do not Change me unless you know 100000% what this value is used for!")
    int getDefaultJACTrustThreshold();

    void setDefaultJACTrustThreshold(int value);

    @AboutConfig
    @DefaultIntValue(15000)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Milliseconds after which a Captcha Dialog will appear even if Auto AntiCaptcha is still running")
    int getCaptchaDialogJAntiCaptchaTimeout();

    void setCaptchaDialogJAntiCaptchaTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(300000)
    @SpinnerValidator(min = 60000, max = Integer.MAX_VALUE)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Milliseconds after which a Captcha Dialog will appear even if Auto 9kw Service is still running")
    int getCaptchaDialog9kwTimeout();

    void setCaptchaDialog9kwTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(300000)
    @SpinnerValidator(min = 60000, max = Integer.MAX_VALUE)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Milliseconds after which a Captcha Dialog will appear even if Auto DeathByCaptcha Service is still running")
    int getCaptchaDialogDBCTimeout();

    void setCaptchaDialogDBCTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(120000)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Milliseconds after which a Captcha Dialog will appear even if Auto CaptchaBroptherhood Service is still running")
    int getCaptchaDialogCaptchaBroptherhoodTimeout2();

    void setCaptchaDialogCaptchaBroptherhoodTimeout2(int ms);

    @AboutConfig
    @DefaultIntValue(120000)
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("Milliseconds after which a Captcha Dialog will appear even if Auto ResolutorCaptcha Service is still running")
    int getCaptchaDialogResolutorCaptchaTimeout();

    void setCaptchaDialogResolutorCaptchaTimeout(int ms);

    @AboutConfig
    @DescriptionForConfigEntry("Enable Captcha Exchangeservices like 9kw or Captchabrotherhood")
    @DefaultBooleanValue(true)
    boolean isCaptchaExchangeServicesEnabled();

    void setCaptchaExchangeServicesEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Disable JAntiCaptcha")
    @DefaultBooleanValue(true)
    boolean isJAntiCaptchaEnabled();

    void setJAntiCaptchaEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Disable all Captcha Dialogs.@See SilentMode Settings")
    @DefaultBooleanValue(true)
    boolean isCaptchaDialogsEnabled();

    void setCaptchaDialogsEnabled(boolean b);

    @AboutConfig
    @DescriptionForConfigEntry("Disable all Remote Captchas. (Android App, IPhone App, Webinterface, ...)")
    @DefaultBooleanValue(true)
    boolean isRemoteCaptchaEnabled();

    void setRemoteCaptchaEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(5 * 60 * 1000)
    @DescriptionForConfigEntry("Timeout after which a challenge (captcha) invalidates (e.g sessions run out...) this timeout should be set by the plugins")
    int getDefaultChallengeTimeout();

    void setDefaultChallengeTimeout(int ms);

    @AboutConfig
    @DefaultIntValue(10000)
    int getChanceToSkipBubbleTimeout();

    void setChanceToSkipBubbleTimeout(int ms);
}
