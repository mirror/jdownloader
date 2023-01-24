package org.jdownloader.captcha.v2.challenge.hcaptcha;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

import org.appwork.utils.Regex;
import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.httpconnection.HTTPConnection.RequestMethod;
import org.jdownloader.logging.LogController;

import jd.controlling.linkcrawler.CrawledLink;
import jd.http.Browser;
import jd.http.Request;
import jd.parser.html.Form;
import jd.plugins.DownloadLink;
import jd.plugins.Plugin;
import jd.plugins.PluginForDecrypt;
import jd.plugins.PluginForHost;

public class AbstractHCaptcha<T extends Plugin> {
    // https://docs.hcaptcha.com/invisible/
    public static enum TYPE {
        NORMAL,
        INVISIBLE
    }

    protected final T            plugin;
    protected final LogInterface logger;
    protected final Browser      br;
    protected String             siteKey;
    private final static String  apiKeyRegex = "[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}";

    public static boolean containsHCaptcha(Browser br) {
        return br != null && containsHCaptcha(br.toString());
    }

    public static boolean containsHCaptcha(String string) {
        return string != null && (new Regex(string, "https?://(\\w+\\.)?hcaptcha\\.com/1/api.js").matches() || new Regex(string, "class\\s*=\\s*('|\")h-captcha(-response)?(\\1|\\s+)").matches());
    }

    public static boolean containsHCaptcha(Form form) {
        return form != null && containsHCaptcha(form.getHtmlCode());
    }

    public final static boolean isValidSiteKey(final String siteKey) {
        return siteKey != null && siteKey.matches("^" + apiKeyRegex + "$");
    }

    public int getSolutionTimeout() {
        /* Last tested: 2021-06-25 */
        return 2 * 60 * 1000;
    }

    public String getSiteDomain() {
        return siteDomain;
    }

    public TYPE getType() {
        return getType(br != null ? br.toString() : null);
    }

    protected TYPE getType(String source) {
        if (source != null) {
            final String[] divs = getDIVs(source);
            if (divs != null) {
                for (final String div : divs) {
                    if (new Regex(div, "class\\s*=\\s*('|\")(?:.*?\\s+)?(g-re|h-)captcha(-response)?(\\1|\\s+)").matches()) {
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

    public AbstractHCaptcha(final T plugin, final Browser br, final String siteKey) {
        this.plugin = plugin;
        this.br = br.cloneBrowser();
        if (br.getRequest() == null) {
            throw new IllegalStateException("Browser.getRequest() == null!");
        }
        this.siteDomain = Browser.getHost(br.getURL(), true);
        logger = createFallbackLogger(plugin);
        this.siteKey = siteKey;
    }

    protected LogInterface createFallbackLogger(T plugin) {
        LogInterface ret = null;
        if (plugin != null) {
            ret = plugin.getLogger();
        }
        if (ret == null) {
            Class<?> cls = getClass();
            String name = cls.getSimpleName();
            while (StringUtils.isEmpty(name)) {
                cls = cls.getSuperclass();
                name = cls.getSimpleName();
            }
            ret = LogController.getInstance().getLogger(name);
        }
        return ret;
    }

    protected void runDdosPrevention() throws InterruptedException {
        if (plugin != null) {
            plugin.runCaptchaDDosProtection(HCaptchaChallenge.getChallengeType());
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
            siteKey = findCorrectSiteKeys(source, br, siteKeys);
            return siteKey;
        }
    }

    protected String findCorrectSiteKeys(final String source, final Browser br, Set<String> siteKeys) {
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
                    if (new Regex(div, "class\\s*=\\s*('|\")(?:.*?\\s+)?(g-re|h-)captcha(-response)?(\\1|\\s+)").matches()) {
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
            // json values in script or json
            // with container, hcaptcha.render(container,parameters)
            String jsSource = new Regex(source, "hcaptcha\\.render\\s*\\(.*?,\\s*\\{(.*?)\\s*\\}\\s*\\)\\s*;").getMatch(0);
            String siteKey = new Regex(jsSource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)\\s*(" + apiKeyRegex + ")\\s*\\2").getMatch(2);
            if (siteKey != null) {
                return siteKey;
            }
            // without, hcaptcha.render(parameters)
            jsSource = new Regex(source, "hcaptcha\\.render\\s*\\(\\s*\\{(.*?)\\s*\\}\\s*\\)\\s*;").getMatch(0);
            siteKey = new Regex(jsSource, "('|\"|)sitekey\\1\\s*:\\s*('|\"|)\\s*(" + apiKeyRegex + ")\\s*\\2").getMatch(2);
            if (siteKey != null) {
                return siteKey;
            }
        }
        {
            // within form
            final Form forms[] = Form.getForms(source);
            if (forms != null) {
                for (final Form form : forms) {
                    final String siteKey = new Regex(form.getHtmlCode(), "data-sitekey\\s*=\\s*('|\")\\s*(" + apiKeyRegex + ")\\s*\\1").getMatch(1);
                    if (siteKey != null) {
                        return siteKey;
                    }
                }
            }
        }
        return null;
    }

    protected HCaptchaChallenge createChallenge() {
        return new HCaptchaChallenge(getSiteKey(), getPlugin(), br, getSiteDomain()) {
            @Override
            public String getSiteUrl() {
                return AbstractHCaptcha.this.getSiteUrl();
            }

            @Override
            public AbstractHCaptcha<T> getAbstractCaptchaHelperHCaptcha() {
                return AbstractHCaptcha.this;
            }

            @Override
            public String getType() {
                final TYPE type = AbstractHCaptcha.this.getType();
                if (type != null) {
                    return type.name();
                } else {
                    return TYPE.NORMAL.name();
                }
            }

            @Override
            protected LogInterface getLogger() {
                return logger;
            }
        };
    }
}