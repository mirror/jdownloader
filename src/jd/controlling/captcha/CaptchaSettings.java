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
    @org.appwork.storage.config.annotations.DescriptionForConfigEntry("True to enable a countdown in captcha dialogs. Dialog will close automated after the coundown")
    boolean isCountdownEnabled();

    void setCountdownEnabled(boolean b);

    @AboutConfig
    @DefaultIntValue(20)
    @SpinnerValidator(min = 0, max = Integer.MAX_VALUE)
    @DescriptionForConfigEntry("Seconds to wait until captcha dialog gets answered. Close dialog after this timeout unanswered")
    int getCountdown();

    void setCountdown(int seconds);

    @DefaultIntValue(0)
    int getLastCancelOption();

    void setLastCancelOption(int i);

}
