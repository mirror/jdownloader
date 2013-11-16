package org.jdownloader.captcha.v2.solver.captchabrotherhood;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;
import org.appwork.storage.config.annotations.DefaultBooleanValue;

public interface CaptchaBrotherHoodSettings extends ConfigInterface {
    @AboutConfig
    String getUser();

    void setUser(String jser);

    @AboutConfig
    String getPass();

    void setPass(String jser);

    @AboutConfig
    @DefaultBooleanValue(false)
    boolean isEnabled();

    void setEnabled(boolean b);
}
