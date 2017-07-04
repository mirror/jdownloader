package org.jdownloader.captcha.v2.challenge.confidentcaptcha;

import jd.http.Browser;
import jd.plugins.Plugin;

import org.appwork.utils.Regex;
import org.appwork.utils.logging2.LogInterface;
import org.jdownloader.logging.LogController;

public abstract class AbstractCaptchaHelperConfidentCaptcha<T extends Plugin> {
    protected T            plugin;
    protected LogInterface logger;
    protected Browser      br;
    protected String       siteKey;

    public AbstractCaptchaHelperConfidentCaptcha(T plugin, Browser br, final String siteKey) {
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
    public String getConfidentCaptchaApiKey() {
        return getConfidentCaptchaApiKey(br != null ? br.toString() : null, br);
    }

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    public static String getConfidentCaptchaApiKey(final String source, final Browser br) {
        final String formData = new Regex(source, "<div[^>]*id=(\"|'|)captcha-dialog\\1[^>]*>.*?</form>\\s*</div>").getMatch(-1);
        return formData;// .replace("confidentincludes/callback.php", "confidentincludes/callback.php");
    }
}
