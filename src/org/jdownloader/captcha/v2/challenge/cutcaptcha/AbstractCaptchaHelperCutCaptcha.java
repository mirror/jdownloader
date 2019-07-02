package org.jdownloader.captcha.v2.challenge.cutcaptcha;

import jd.http.Browser;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperCutCaptcha<T extends Plugin> {
    protected final T            plugin;
    protected final LogInterface logger;
    protected final Browser      br;
    protected final String       siteKey;

    public AbstractCaptchaHelperCutCaptcha(T plugin, Browser br, String siteKey) {
        this.plugin = plugin;
        if (br.getRequest() == null) {
            throw new IllegalStateException("Browser.getRequest() == null!");
        } else {
            this.br = br;
        }
        if (plugin.getLogger() == null) {
            logger = LogController.getInstance().getLogger(getClass().getSimpleName());
        } else {
            logger = plugin.getLogger();
        }
        this.siteKey = siteKey;
    }

    public T getPlugin() {
        return plugin;
    }

    public String getSiteKey() {
        return siteKey;
    }

    protected Browser getBrowser() {
        return br;
    }

    public LogInterface getLogger() {
        return logger;
    }

    protected CutCaptchaChallenge createChallenge() throws PluginException {
        final T plugin = getPlugin();
        final String siteKey = getSiteKey();
        if (plugin == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (siteKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return new CutCaptchaChallenge(siteKey, plugin) {
                @Override
                public BrowserViewport getBrowserViewport(BrowserWindow screenResource, java.awt.Rectangle elementBounds) {
                    return null;
                }
            };
        }
    }
}
