package org.jdownloader.captcha.v2.solver;

import org.appwork.storage.config.ConfigInterface;
import org.appwork.storage.config.annotations.AboutConfig;

public interface CaptchaBrotherHoodSettings extends ConfigInterface {
    @AboutConfig
    String getUser();

    void setUser(String jser);

    @AboutConfig
    String getPass();

    void setPass(String jser);
}
