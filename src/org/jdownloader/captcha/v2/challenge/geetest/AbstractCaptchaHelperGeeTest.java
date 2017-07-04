package org.jdownloader.captcha.v2.challenge.geetest;

import jd.http.Browser;
import jd.parser.html.Form;
import jd.plugins.LinkStatus;
import jd.plugins.Plugin;
import jd.plugins.PluginException;

import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperGeeTest<T extends Plugin> {
    protected T            plugin;
    protected LogInterface logger;
    protected Browser      br;
    protected String       siteKey;

    public AbstractCaptchaHelperGeeTest(T plugin, Browser br, String siteKey) {
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
    protected String getGeeTestApiKey() {
        return getGeeTestApiKey(br != null ? br.toString() : null);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    protected static String getGeeTestApiKey(final String source) {
        if (source == null) {
            return null;
        }
        // lets look for default
        final String[] scripts = new Regex(source, "<script[^>]*>.*?</script>").getColumn(-1);
        if (scripts != null) {
            for (final String script : scripts) {
                final String apiKey = new Regex(script, "src=('|\")https?://api\\.geetest\\.com/[^\\s]*(?:\\?|&)gt=([a-f0-9]{32})[^\\s]*\\1").getMatch(1);
                if (apiKey != null) {
                    return apiKey;
                }
            }
        }
        return null;
    }

    public static boolean containsGeeTest(final Browser br) throws PluginException {
        if (br == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String geeTestApiKey = getGeeTestApiKey(br.toString());
        return geeTestApiKey != null ? true : false;
    }

    public static boolean containsGeeTest(final Form form) throws PluginException {
        if (form == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String geeTestApiKey = getGeeTestApiKey(form.getHtmlCode());
        return geeTestApiKey != null ? true : false;
    }

    public static boolean containsGeeTest(final String string) throws PluginException {
        if (string == null) {
            throw new PluginException(LinkStatus.ERROR_PLUGIN_DEFECT);
        }
        final String geeTestApiKey = getGeeTestApiKey(string);
        return geeTestApiKey != null ? true : false;
    }
}
