package org.jdownloader.captcha.v2.challenge.sweetcaptcha;

import jd.http.Browser;
import jd.nutils.encoding.Encoding;
import jd.parser.html.Form;
import jd.plugins.Plugin;

import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperSweetCaptcha<T extends Plugin> {
    protected T            plugin;
    protected LogInterface logger;
    protected Browser      br;
    protected String       siteKey;
    protected String       appKey;

    public AbstractCaptchaHelperSweetCaptcha(T plugin, Browser br, final String siteKey, final String appKey) {
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
        this.appKey = appKey;
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
    public String getSweetCaptchaApiKey() {
        return getSweetCaptchaApiKey(br != null ? br.toString() : null);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public static String getSweetCaptchaApiKey(final String source) {
        if (source == null) {
            return null;
        }
        String apiKey = new Regex(source, "<div[^>]+id=(?:\"|'|)(sc_[a-f0-9]{7})").getMatch(0);
        return apiKey;
    }

    /**
     *
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public String getSweetCaptchaAppKey() {
        return getSweetCaptchaAppKey(br != null ? br.toString() : null);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public static String getSweetCaptchaAppKey(final String source) {
        if (source == null) {
            return null;
        }
        String apiKey = new Regex(source, "sweetcaptcha\\.com/api/v2/apps/(\\d+)/captcha/sc_").getMatch(0);
        return apiKey;
    }

    public Form setFormValues(final Form form, final String results) {
        // results can not be null, as check is done within getToken
        final String[][] args = new Regex(results, "\\[\\s*\"(.*?)\"\\s*,\\s*\"(.*?)\"\\s*\\]").getMatches();
        for (final String[] arg : args) {
            form.put(arg[0], Encoding.urlEncode(arg[1]));
        }
        return form;
    }
}
