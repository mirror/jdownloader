package org.jdownloader.captcha.v2;

import jd.plugins.PluginException;

public interface CaptchaHosterHelperInterface {
    public String getToken() throws PluginException, InterruptedException;
}
