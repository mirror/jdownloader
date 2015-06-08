package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.util.logging.Logger;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.Regex;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperRecaptchaV2<T extends Plugin> {
    protected T       plugin;
    protected Logger  logger;
    protected Browser br;
    protected String  siteKey;

    public AbstractCaptchaHelperRecaptchaV2(T plugin, Browser br, String siteKey) {
        this.plugin = plugin;
        this.br = br;
        logger = plugin.getLogger();
        if (logger == null) {
            logger = LogController.getInstance().getLogger(getClass().getSimpleName());
        }
        this.siteKey = siteKey;
    }

    public T getPlugin() {
        return plugin;
    }

    /**
     *
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public String getRecaptchaV2ApiKey() {
        return getRecaptchaV2ApiKey(br != null ? br.toString() : null);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public String getRecaptchaV2ApiKey(final String source) {
        String apiKey = null;
        if (source == null) {
            return null;
        }
        // lets look for default
        final String[] divs = new Regex(source, "<div[^>]*>.*?</div>").getColumn(-1);
        if (divs != null) {
            for (final String div : divs) {
                if (new Regex(div, "class=('|\")g-recaptcha\\1").matches()) {
                    apiKey = new Regex(div, "data-sitekey=('|\")([\\w-]+)\\1").getMatch(1);
                    if (apiKey != null) {
                        break;
                    }
                }
            }
        }
        if (apiKey == null) {
            final String jssource = new Regex(source, "grecaptcha\\.render\\(.*?, \\{(.*?)\\}\\);").getMatch(0);
            if (jssource != null) {
                apiKey = new Regex(jssource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)([\\w-]+)\\2").getMatch(2);
            }
        }
        return apiKey;
    }

}
