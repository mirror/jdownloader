package org.jdownloader.settings;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface SoundSettings extends ConfigInterface {
    @DefaultBooleanValue(true)
    @AboutConfig
    @DescriptionForConfigEntry("Play a Sound when a Captchadialog opens")
    boolean isCaptchaSoundEnabled();

    void setCaptchaSoundEnabled(boolean b);

}
