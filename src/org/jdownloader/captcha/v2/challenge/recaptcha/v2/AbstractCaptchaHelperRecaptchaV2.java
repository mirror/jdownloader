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
    protected String       secureToken;

    public String getSecureToken() {
        return getSecureToken(br != null ? br.toString() : null);
    }

    public String getSecureToken(final String source) {
        if (secureToken != null) {
            return secureToken;
        }
        // from fallback url
        secureToken = new Regex(source, "&stoken=([^\"]+)").getMatch(0);
        if (secureToken == null) {
            secureToken = new Regex(source, "data-stoken\\s*=\\s*\"([^\"]+)").getMatch(0);
        }

        return secureToken;
    }

    public void setSecureToken(String secureToken) {
        this.secureToken = secureToken;
    }

    private String siteUrl;

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
            if (CORRECT_AFTER_LIST.size() == 0) {
                return 3;
            }
            int min = Integer.MAX_VALUE;
            for (int i : CORRECT_AFTER_LIST) {
                min = Math.min(i, min);
            }
            return min;
        }
    }

    private String siteDomain;

    public AbstractCaptchaHelperRecaptchaV2(final T plugin, final Browser br, final String siteKey, final String secureToken) {
        this.plugin = plugin;
        this.br = br.cloneBrowser();
        this.siteUrl = br.getURL();
        this.siteDomain = Browser.getHost(siteUrl, true);
        logger = plugin.getLogger();
        if (logger == null) {
            logger = LogController.getInstance().getLogger(getClass().getSimpleName());
        }
        this.siteKey = siteKey;
        this.secureToken = secureToken;
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
    public String getSiteKey() {
        return getSiteKey(br != null ? br.toString() : null);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public String getSiteKey(final String source) {
        if (siteKey != null) {
            return siteKey;
        }
        if (source == null) {
            return null;
        }
        // lets look for default
        final String[] divs = new Regex(source, "<div(?:[^>]*>.*?</div>|[^>]*\\s*/\\s*>)").getColumn(-1);
        if (divs != null) {
            for (final String div : divs) {
                if (new Regex(div, "class=('|\")g-recaptcha\\1").matches()) {
                    siteKey = new Regex(div, "data-sitekey=('|\")([\\w-]+)\\1").getMatch(1);
                    if (siteKey != null) {
                        return siteKey;
                    }
                }
            }
        }
        // can also be within <script> (for example cloudflare)
        final String[] scripts = new Regex(source, "<\\s*script\\s+(?:.*?<\\s*/\\s*script\\s*>|[^>]+\\s*/\\s*>)").getColumn(-1);
        if (scripts != null) {
            for (final String script : scripts) {
                siteKey = new Regex(script, "data-sitekey=('|\")([\\w-]+)\\1").getMatch(1);
                if (siteKey != null) {
                    return siteKey;
                }
            }
        }
        // within iframe
        final String[] iframes = new Regex(source, "<\\s*iframe\\s+(?:.*?<\\s*/\\s*iframe\\s*>|[^>]+\\s*/\\s*>)").getColumn(-1);
        if (iframes != null) {
            for (final String iframe : iframes) {
                siteKey = new Regex(iframe, "google\\.com/recaptcha/api/fallback\\?k=([\\w-]+)").getMatch(0);
                if (siteKey != null) {
                    return siteKey;
                }
            }
        }
        // json values in script or json
        final String jssource = new Regex(source, "recaptcha\\.render\\(.*?, \\{(.*?)\\}\\);").getMatch(0);
        if (jssource != null) {
            siteKey = new Regex(jssource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)([\\w-]+)\\2").getMatch(2);
        }
        return siteKey;
    }

}
