package org.jdownloader.captcha.v2.challenge.recaptcha.v2;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.encoding.Base64;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.jdownloader.logging.LogController;

public abstract class AbstractRecaptchaV2<T extends Plugin> {
    public static enum TYPE {
        NORMAL,
        INVISIBLE
    }

    protected final T       plugin;
    protected LogInterface  logger;
    protected final Browser br;
    protected String        siteKey;
    protected String        secureToken;

    public String getSecureToken() {
        return getSecureToken(br != null ? br.toString() : null);
    }

    public static boolean containsRecaptchaV2Class(Browser br) {
        return br != null && containsRecaptchaV2Class(br.toString());
    }

    public static boolean containsRecaptchaV2Class(String string) {
        // class="g-recaptcha-response"
        // class="g-recaptcha"
        // grecaptcha.execute RecaptchaV3 support
        return string != null && new Regex(string, "class\\s*=\\s*('|\")g-recaptcha(-response)?(\\1|\\s+)").matches();
    }

    public static boolean containsRecaptchaV2Class(Form form) {
        // change naming to contains RecaptchaClass oder V2V3 Class
        return form != null && containsRecaptchaV2Class(form.getHtmlCode());
    }

    /**
     * 2019-07-03: This is the time for which the g-recaptcha-token can be used AFTER a user has solved a challenge. You can easily check
     * the current value by opening up a website which requires a reCaptchaV2 captcha which does not auto-confirm after solving (e.g. you
     * have to click a "send" button afterwards). To this date, the challenge will invalidate itself after 120 seconds - it will display and
     * errormessage and the user will have to solve it again! This value is especially important for rare EDGE cases such as long
     * waiting-times + captcha. Example: User has to wait 180 seconds before he can confirm such a captcha. If he solves it directly, the
     * captcha will be invalid once the 180 seconds are over. Also see documentation in XFileSharingProBasic.java class in method
     * 'handleCaptcha'. </br> TRY TO KEEP THIS VALUE UP-TO-DATE!!
     */
    public int getSolutionTimeout() {
        return 1 * 60 * 1000;
    }

    protected Map<String, Object> getV3Action() {
        return getV3Action(br != null ? br.toString() : null);
    }

    protected Map<String, Object> getV3Action(final String source) {
        if (source != null) {
            final String actionJson = new Regex(source, "grecaptcha\\.execute\\s*\\([^{]*,\\s*(\\{.*?\\}\\s*)").getMatch(0);
            final String action = new Regex(actionJson, "action(?:\"|')?\\s*:\\s*(?:\"|')(.*?)(\"|')").getMatch(0);
            if (action != null) {
                final Map<String, Object> ret = new HashMap<String, Object>();
                ret.put("action", action);
                return ret;
            }
        }
        return null;
    }

    protected String getSecureToken(final String source) {
        if (secureToken == null) {
            // from fallback url
            secureToken = new Regex(source, "&stoken=([^\"]+)").getMatch(0);
            if (secureToken == null) {
                secureToken = new Regex(source, "data-stoken\\s*=\\s*\"\\s*([^\"]+)").getMatch(0);
            }
        }
        return secureToken;
    }

    public TYPE getType() {
        return getType(br != null ? br.toString() : null);
    }

    protected TYPE getType(String source) {
        if (source != null) {
            if (getV3Action(source) != null) {
                return TYPE.INVISIBLE;
            }
            final String[] divs = getDIVs(source);
            if (divs != null) {
                for (final String div : divs) {
                    if (new Regex(div, "class\\s*=\\s*('|\")(?:.*?\\s+)?g-recaptcha(-response)?(\\1|\\s+)").matches()) {
                        final String siteKey = new Regex(div, "data-sitekey\\s*=\\s*('|\")\\s*(" + apiKeyRegex + ")\\s*\\1").getMatch(1);
                        if (siteKey != null && StringUtils.equals(siteKey, getSiteKey())) {
                            final boolean isInvisible = new Regex(div, "data-size\\s*=\\s*('|\")\\s*(invisible)\\s*\\1").matches();
                            if (isInvisible) {
                                return TYPE.INVISIBLE;
                            }
                        }
                    }
                }
            }
        }
        return TYPE.NORMAL;
    }

    public void setSecureToken(String secureToken) {
        this.secureToken = secureToken;
    }

    public String getSiteDomain() {
        return siteDomain;
    }

    protected String getSiteUrl() {
        final String siteDomain = getSiteDomain();
        String url = null;
        final Request request = br != null ? br.getRequest() : null;
        final boolean canUseRequestURL = request != null && request.getHttpConnection() != null && RequestMethod.GET.equals(request.getRequestMethod()) && StringUtils.containsIgnoreCase(request.getHttpConnection().getContentType(), "html");
        boolean rewriteHost = true;
        String defaultProtocol = "http://";
        if (plugin != null) {
            if (plugin.getMatcher().pattern().pattern().matches(".*(https?).*")) {
                defaultProtocol = "https://";
            }
            if (plugin instanceof PluginForHost) {
                final DownloadLink downloadLink = ((PluginForHost) plugin).getDownloadLink();
                if (downloadLink != null) {
                    url = downloadLink.getPluginPatternMatcher();
                }
            } else if (plugin instanceof PluginForDecrypt) {
                final CrawledLink crawledLink = ((PluginForDecrypt) plugin).getCurrentLink();
                if (crawledLink != null) {
                    url = crawledLink.getURL();
                }
            }
            if (url != null && request != null) {
                final String referer = request.getHeaders().getValue("Referer");
                if (referer != null && plugin.canHandle(referer) && canUseRequestURL) {
                    rewriteHost = false;
                    url = request.getUrl();
                } else {
                    url = url.replaceAll("^(?i)(https?://)", request.getURL().getProtocol() + "://");
                }
            }
            if (StringUtils.equals(url, siteDomain) || StringUtils.equals(url, plugin.getHost())) {
                if (request != null) {
                    url = request.getURL().getProtocol() + "://" + url;
                } else {
                    url = defaultProtocol + url;
                }
            }
        }
        if (url == null && request != null && canUseRequestURL) {
            url = request.getUrl();
        }
        if (url != null) {
            // remove anchor
            url = url.replaceAll("(#.+)", "");
            final String urlDomain = Browser.getHost(url, true);
            if (rewriteHost && !StringUtils.equalsIgnoreCase(urlDomain, siteDomain)) {
                url = url.replaceFirst(Pattern.quote(urlDomain), siteDomain);
            }
            return url;
        } else {
            if (request != null) {
                return request.getURL().getProtocol() + "://" + siteDomain;
            } else {
                return defaultProtocol + siteDomain;
            }
        }
    }

    private final String siteDomain;

    public AbstractRecaptchaV2(final T plugin, final Browser br, final String siteKey, final String secureToken, boolean boundToDomain) {
        this.plugin = plugin;
        this.br = br.cloneBrowser();
        if (br.getRequest() == null) {
            throw new IllegalStateException("Browser.getRequest() == null!");
        }
        this.siteDomain = Browser.getHost(br.getURL(), true);
        logger = plugin == null ? null : plugin.getLogger();
        if (logger == null) {
            createFallbackLogger();
        }
        this.siteKey = siteKey;
        this.secureToken = secureToken;
    }

    protected void createFallbackLogger() {
        Class<?> cls = getClass();
        String name = cls.getSimpleName();
        while (StringUtils.isEmpty(name)) {
            cls = cls.getSuperclass();
            name = cls.getSimpleName();
        }
        logger = LogController.getInstance().getLogger(name);
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

    private final static String apiKeyRegex = "[\\w-_]{6,}";

    protected String[] getDIVs(String source) {
        return new Regex(source, "<\\s*(div|button)(?:[^>]*>.*?</\\1>|[^>]*\\s*/\\s*>)").getColumn(-1);
    }

    protected static final HashSet<String> INVALID_SITE_KEYS = new HashSet<String>();

    /**
     * will auto find api key, based on google default &lt;div&gt;, @Override to make customised finder.
     *
     * @author raztoki
     * @since JD2
     * @return
     */
    protected String getSiteKey(final String source) {
        if (siteKey != null) {
            return siteKey;
        } else if (source == null) {
            return null;
        } else {
            String findNextSiteKeySource = source;
            final HashSet<String> siteKeys = new HashSet<String>();
            while (StringUtils.isNotEmpty(findNextSiteKeySource)) {
                final String siteKey = findNextSiteKey(findNextSiteKeySource);
                if (siteKey != null) {
                    siteKeys.add(siteKey);
                    findNextSiteKeySource = findNextSiteKeySource.replace(siteKey, "");
                } else {
                    break;
                }
            }
            synchronized (INVALID_SITE_KEYS) {
                logger.info("Auto siteKeys unfiltered:" + siteKeys);
                siteKeys.removeAll(INVALID_SITE_KEYS);
                logger.info("Auto siteKeys filtered:" + siteKeys);
            }
            siteKey = findCorretSiteKeys(source, br, siteKeys);
            return siteKey;
        }
    }

    protected String findCorretSiteKeys(final String source, final Browser br, Set<String> siteKeys) {
        if (siteKeys.size() == 0) {
            logger.info("No siteKey found!");
            return null;
        } else if (siteKeys.size() == 1) {
            final String siteKey = siteKeys.iterator().next();
            logger.info("Auto single siteKey:" + siteKey);
            return siteKey;
        } else {
            logger.info("Auto multiple siteKeys:" + siteKeys);
            if (br == null) {
                final String siteKey = siteKeys.iterator().next();
                logger.info("No browser available?! Use first known siteKey:" + siteKey);
                return siteKey;
            } else {
                final URL url = br._getURL();
                final String co = Base64.encode(url.getProtocol() + "://" + url.getHost() + ":" + (url.getPort() == -1 ? url.getDefaultPort() : url.getPort())).replace("=", ".");
                final Iterator<String> it = siteKeys.iterator();
                while (it.hasNext()) {
                    final Browser brc = br.cloneBrowser();
                    final String siteKey = it.next();
                    try {
                        brc.getPage("https://www.google.com/recaptcha/api2/anchor?k=" + siteKey + "&co=" + co);
                        if (brc.containsHTML("Invalid site key")) {
                            synchronized (INVALID_SITE_KEYS) {
                                INVALID_SITE_KEYS.add(siteKey);
                            }
                            logger.info("SiteKey:" + siteKey + " seems to be invalid!");
                            it.remove();
                        } else {
                            logger.info("SiteKey:" + siteKey + " seems to be valid!");
                            return siteKey;
                        }
                    } catch (IOException e) {
                        logger.log(e);
                    }
                }
                logger.info("Could not auto find siteKey!");
                return null;
            }
        }
    }

    protected String findNextSiteKey(String source) {
        {
            // lets look for defaults
            final String[] divs = getDIVs(source);
            if (divs != null) {
                for (final String div : divs) {
                    if (new Regex(div, "class\\s*=\\s*('|\")(?:.*?\\s+)?g-recaptcha(-response)?(\\1|\\s+)").matches()) {
                        final String siteKey = new Regex(div, "data-sitekey\\s*=\\s*('|\")\\s*(" + apiKeyRegex + ")\\s*\\1").getMatch(1);
                        if (siteKey != null) {
                            return siteKey;
                        }
                    }
                }
            }
        }
        {
            // can also be within <script> (for example cloudflare)
            final String[] scripts = new Regex(source, "<\\s*script\\s+(?:.*?<\\s*/\\s*script\\s*>|[^>]+\\s*/\\s*>)").getColumn(-1);
            if (scripts != null) {
                for (final String script : scripts) {
                    final String siteKey = new Regex(script, "data-sitekey\\s*=\\s*('|\")\\s*(" + apiKeyRegex + ")\\s*\\1").getMatch(1);
                    if (siteKey != null) {
                        return siteKey;
                    }
                }
            }
        }
        {
            // within iframe
            final String[] iframes = new Regex(source, "<\\s*iframe\\s+(?:.*?<\\s*/\\s*iframe\\s*>|[^>]+\\s*/\\s*>)").getColumn(-1);
            if (iframes != null) {
                for (final String iframe : iframes) {
                    final String siteKey = new Regex(iframe, "google\\.com/recaptcha/api/fallback\\?k=(" + apiKeyRegex + ")").getMatch(0);
                    if (siteKey != null) {
                        return siteKey;
                    }
                }
            }
        }
        {
            // json values in script or json
            // with container, grecaptcha.render(container,parameters), eg RecaptchaV2
            String jsSource = new Regex(source, "recaptcha\\.render\\s*\\(.*?,\\s*\\{(.*?)\\s*\\}\\s*\\)\\s*;").getMatch(0);
            String siteKey = new Regex(jsSource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)\\s*(" + apiKeyRegex + ")\\s*\\2").getMatch(2);
            if (siteKey != null) {
                return siteKey;
            }
            // without, grecaptcha.render(parameters), eg RecaptchaV3
            jsSource = new Regex(source, "recaptcha\\.render\\s*\\(\\s*\\{(.*?)\\s*\\}\\s*\\)\\s*;").getMatch(0);
            siteKey = new Regex(jsSource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)\\s*(" + apiKeyRegex + ")\\s*\\2").getMatch(2);
            if (siteKey != null) {
                return siteKey;
            }
        }
        {
            // RecaptchaV3, grecaptcha.execute(apiKey)
            final String siteKey = new Regex(source, "grecaptcha\\.execute\\s*\\(\\s*('|\")\\s*(" + apiKeyRegex + ")\\s*\\1").getMatch(1);
            if (siteKey != null) {
                return siteKey;
            }
        }
        return null;
    }

    protected RecaptchaV2Challenge createChallenge() {
        return new RecaptchaV2Challenge(getSiteKey(), getSecureToken(), getPlugin(), br, getSiteDomain()) {
            @Override
            public String getSiteUrl() {
                return AbstractRecaptchaV2.this.getSiteUrl();
            }

            @Override
            public AbstractRecaptchaV2<T> getAbstractCaptchaHelperRecaptchaV2() {
                return AbstractRecaptchaV2.this;
            }

            @Override
            public Map<String, Object> getV3Action() {
                return AbstractRecaptchaV2.this.getV3Action();
            }

            @Override
            public String getType() {
                final TYPE type = AbstractRecaptchaV2.this.getType();
                if (type != null) {
                    return type.name();
                } else {
                    return TYPE.NORMAL.name();
                }
            }
        };
    }
}
