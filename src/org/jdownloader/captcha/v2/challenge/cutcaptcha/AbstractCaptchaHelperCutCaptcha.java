package org.jdownloader.captcha.v2.challenge.cutcaptcha;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperCutCaptcha<T extends Plugin> {
    protected T            plugin;
    protected LogInterface logger;
    protected Browser      br;
    protected String       siteKey;

    public AbstractCaptchaHelperCutCaptcha(T plugin, Browser br, String siteKey) {
        this.plugin = plugin;
        this.br = br;
        if (br.getRequest() == null) {
            throw new IllegalStateException("Browser.getRequest() == null!");
        }
        logger = plugin.getLogger();
        if (logger == null) {
            logger = LogController.getInstance().getLogger(getClass().getSimpleName());
        }
        this.siteKey = siteKey;
    }

    public T getPlugin() {
        return plugin;
    }
}
