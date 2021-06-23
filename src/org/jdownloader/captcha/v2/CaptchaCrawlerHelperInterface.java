package org.jdownloader.captcha.v2;

import jd.plugins.DecrypterException;
import jd.plugins.PluginException;

public interface CaptchaCrawlerHelperInterface {
    public String getToken() throws PluginException, InterruptedException, DecrypterException;
}
