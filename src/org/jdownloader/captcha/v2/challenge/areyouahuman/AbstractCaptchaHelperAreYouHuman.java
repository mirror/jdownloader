package org.jdownloader.captcha.v2.challenge.areyouahuman;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperAreYouHuman<T extends Plugin> {
    protected T            plugin;
    protected LogInterface logger;
    protected Browser      br;
    protected String       siteKey;

    public AbstractCaptchaHelperAreYouHuman(T plugin, Browser br, String siteKey) {
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

    /**
     *
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    protected String getAreYouAHumanApiKey() {
        return getAreYouAHumanApiKey(br != null ? br.toString() : null);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    protected String getAreYouAHumanApiKey(final String source) {
        if (source == null) {
            return null;
        }
        // lets look for default
        final String[] scripts = new Regex(source, "<script[^>]*>.*?</script>").getColumn(-1);
        if (scripts != null) {
            for (final String script : scripts) {
                final String apiKey = new Regex(script, "src=('|\")https?://ws\\.areyouahuman\\.com/ws/script/([a-f0-9]{40})\\1").getMatch(1);
                if (apiKey != null) {
                    return apiKey;
                }
            }
        }
        return null;
    }
}
