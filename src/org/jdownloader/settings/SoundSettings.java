package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DefaultIntValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;
import org.appwork.storage.config.annotations.SpinnerValidator;

public interface SoundSettings extends ConfigInterface {
    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("Play a Sound when a Captchadialog opens")
    boolean isCaptchaSoundEnabled();

    void setCaptchaSoundEnabled(boolean b);

    @DefaultIntValue(100)
    @SpinnerValidator(min = 0, max = 100)
    @AboutConfig
    int getCaptchaSoundVolume();

    void setCaptchaSoundVolume(int percent);

}
