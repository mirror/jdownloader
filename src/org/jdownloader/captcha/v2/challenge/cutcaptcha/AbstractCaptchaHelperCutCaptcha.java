package org.jdownloader.captcha.v2.challenge.cutcaptcha;

import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.captcha.v2.solver.browser.BrowserViewport;
import org.jdownloader.captcha.v2.solver.browser.BrowserWindow;
import org.jdownloader.logging.LogController;

import jd.http.Browser;
import jd.parser.Regex;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

public abstract class AbstractCaptchaHelperCutCaptcha<T extends Plugin> {
    protected final T            plugin;
    protected final LogInterface logger;
    protected final Browser      br;
    protected String             siteKey;
    protected String             apiKey;

    public AbstractCaptchaHelperCutCaptcha(T plugin, Browser br) {
        this(plugin, br, null, null);
    }

    @Deprecated
    public AbstractCaptchaHelperCutCaptcha(T plugin, Browser br, String siteKey) {
        this(plugin, br, siteKey, null);
    }

    public AbstractCaptchaHelperCutCaptcha(final T plugin, final Browser br, final String siteKey, final String apiKey) {
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
        this.apiKey = apiKey;
    }

    public T getPlugin() {
        return plugin;
    }

    public String getSiteKey() {
        return getSiteKey(br.getRequest().getHtmlCode());
    }

    public String getSiteKey(final String source) {
        if (siteKey != null) {
            return siteKey;
        } else {
            /* Auto find sitekey */
            final String autoSiteKey = new Regex(source, "CUTCAPTCHA_MISERY_KEY = \"([a-f0-9]{40})").getMatch(0);
            if (autoSiteKey != null) {
                this.siteKey = autoSiteKey;
                return this.siteKey;
            } else {
                logger.info("AutoSiteKey: No siteKey found!");
                return null;
            }
        }
    }

    public String getAPIKey() {
        return getAPIKey(br.getRequest().getHtmlCode());
    }

    public String getAPIKey(final String source) {
        if (apiKey != null) {
            return apiKey;
        } else {
            /* Auto find sitekey */
            final String autoApiKey = new Regex(source, "cutcaptcha\\.net/captcha/([A-Za-z0-9]+)\\.js").getMatch(0);
            if (autoApiKey != null) {
                this.apiKey = autoApiKey;
                return this.apiKey;
            } else {
                logger.info("AutoApiKey: No apiKey found!");
                return null;
            }
        }
    }

    /** Returns URL of the page where the captcha is displayed at. */
    public String getPageURL() {
        return br.getURL();
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
        final String apiKey = getAPIKey();
        if (plugin == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (siteKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else if (apiKey == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        } else {
            return new CutCaptchaChallenge(plugin, siteKey, apiKey) {
                @Override
                public BrowserViewport getBrowserViewport(BrowserWindow screenResource, java.awt.Rectangle elementBounds) {
                    return null;
                }
            };
        }
    }
}
