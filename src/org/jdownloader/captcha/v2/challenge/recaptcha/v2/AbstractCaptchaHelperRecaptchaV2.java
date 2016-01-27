package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.util.ArrayList;

import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

import jd.http.Browser;
import jd.plugins.Plugin;

public abstract class AbstractCaptchaHelperRecaptchaV2<T extends Plugin> {
    protected T            plugin;
    protected LogInterface logger;
    protected Browser      br;
    protected String       siteKey;
    private String         siteUrl;

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getSiteDomain() {
        return siteDomain;
    }

    private static ArrayList<Integer> CORRECT_AFTER_LIST = new ArrayList<Integer>();

    protected static void setCorrectAfter(int size) {
        synchronized (CORRECT_AFTER_LIST) {
            CORRECT_AFTER_LIST.add(size);
            while (CORRECT_AFTER_LIST.size() > 5) {
                CORRECT_AFTER_LIST.remove(0);
            }
        }
    }

    protected static int getRequiredCorrectAnswersGuess() {

        synchronized (CORRECT_AFTER_LIST) {
            if (CORRECT_AFTER_LIST.size() < 5) {
                return 1;
            }
            int min = Integer.MAX_VALUE;
            for (int i : CORRECT_AFTER_LIST) {
                min = Math.min(i, min);
            }
            return Math.min(2, min);
        }
    }

    private String siteDomain;

    public AbstractCaptchaHelperRecaptchaV2(T plugin, Browser br, String siteKey) {
        this.plugin = plugin;
        this.br = br;
        this.siteUrl = br.getURL();
        this.siteDomain = Browser.getHost(siteUrl, true);
        logger = plugin.getLogger();
        if (logger == null) {
            logger = LogController.getInstance().getLogger(getClass().getSimpleName());
        }
        this.siteKey = siteKey;

    }

    protected void runDdosPrevention() throws InterruptedException {
        if (plugin != null) {
            plugin.runCaptchaDDosProtection(RecaptchaV2Challenge.RECAPTCHAV2);
        }
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
            final String jssource = new Regex(source, "recaptcha\\.render\\(.*?, \\{(.*?)\\}\\);").getMatch(0);
            if (jssource != null) {
                apiKey = new Regex(jssource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)([\\w-]+)\\2").getMatch(2);
            }
        }
        return apiKey;
    }

}
