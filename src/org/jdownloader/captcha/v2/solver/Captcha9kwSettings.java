package org.jdownloader.captcha.v2.solver;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;
import org.appwork.storage.config.annotations.DescriptionForConfigEntry;

public interface Captcha9kwSettings extends ConfigInterface {
    @AboutConfig
    @DescriptionForConfigEntry("Your (User) ApiKey from 9kw.eu")
    String getApiKey();

    void setApiKey(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Active the 9kw.eu service")
    boolean isEnabled();

    void setEnabled(boolean b);

    @AboutConfig
    @DefaultBooleanValue(false)
    @DescriptionForConfigEntry("Active the Mouse Captchas")
    boolean ismouse();

    void setmouse(boolean b);
}
