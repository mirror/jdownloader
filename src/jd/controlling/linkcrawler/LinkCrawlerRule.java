package jd.controlling.linkcrawler;

import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;

import org.appwork.utils.StringUtils;
import org.appwork.utils.logging2.LogInterface;
import org.appwork.utils.net.URLHelper;
import org.jdownloader.controlling.UniqueAlltimeID;

import jd.http.Browser;
import jd.http.Cookie;
import jd.http.Cookies;

public class LinkCrawlerRule {
    public static enum RULE {
        REWRITE,
        SUBMITFORM,
        DIRECTHTTP,
        DEEPDECRYPT,
        FOLLOWREDIRECT
    }

    protected boolean                    enabled          = true;
    protected Object                     cookies          = null;
    protected List<String[]>             headers          = null;
    protected Map<String, List<Pattern>> propertyPatterns = null;
    protected boolean                    updateCookies    = false;
    protected boolean                    logging          = false;

    public boolean isLogging() {
        return logging;
    }

    public void setLogging(boolean logging) {
        this.logging = logging;
    }

    public boolean isUpdateCookies() {
        return updateCookies;
    }

    public void setUpdateCookies(boolean updateCookies) {
        this.updateCookies = updateCookies;
    }

    public List<String[]> getCookiesList() {
        if (!(this.cookies instanceof List)) {
            return null;
        }
        return (List<String[]>) this.cookies;
    }

    public Object getCookies() {
        return cookies;
    }

    public void setCookies(final Object obj) {
        final Cookies cookies = Cookies.parseCookiesFromObject(obj, null);
        if (cookies == null) {
            setCookiesList(null);
            return;
        }
        final List<String[]> cookielist = new ArrayList<String[]>();
        for (final Cookie cookie : cookies.getCookies()) {
            if (cookie.getHost() != null) {
                final String[] cookiearray = new String[] { cookie.getKey(), cookie.getValue(), cookie.getHost() };
                cookielist.add(cookiearray);
            } else {
                final String[] cookiearray = new String[] { cookie.getKey(), cookie.getValue() };
                cookielist.add(cookiearray);
            }
        }
        setCookiesList(cookielist);
    }

    public void setCookiesList(final List<String[]> cookies) {
        this.cookies = cookies;
    }

    public List<String[]> getHeaders() {
        return headers;
    }

    public void setHeaders(List<String[]> headers) {
        this.headers = headers;
    }

    public Map<String, Object> getPropertyPatterns() {
        if (propertyPatterns == null) {
            return null;
        }
        final Map<String, Object> propertyPatternsO = new HashMap<String, Object>();
        for (final Entry<String, List<Pattern>> entry : propertyPatterns.entrySet()) {
            final List<String> patternsStr = new ArrayList<String>();
            for (final Pattern pattern : entry.getValue()) {
                patternsStr.add(pattern.pattern());
            }
            propertyPatternsO.put(entry.getKey(), patternsStr);
        }
        return propertyPatternsO;
    }

    public Map<String, List<Pattern>> _getPropertyPatterns() {
        return propertyPatterns;
    }

    public void setPropertyPatterns(final Map<String, Object> propertyPatterns) {
        if (propertyPatterns == null) {
            this.propertyPatterns = null;
            return;
        }
        final Map<String, List<Pattern>> compiledPropertiesPatternMap = new HashMap<String, List<Pattern>>();
        for (final Entry<String, Object> entry : propertyPatterns.entrySet()) {
            final String key = entry.getKey();
            if (StringUtils.isEmpty(key)) {
                /* Invalid item */
                continue;
            }
            final Object value = entry.getValue();
            final List<Pattern> patterns = new ArrayList<Pattern>();
            if (value instanceof String) {
                patterns.add(Pattern.compile(value.toString()));
            } else if (value instanceof List) {
                for (final String regexStr : (List<String>) value) {
                    patterns.add(Pattern.compile(regexStr));
                }
            } else {
                /* Ignore */
                continue;
            }
            compiledPropertiesPatternMap.put(key, patterns);
        }
        this.propertyPatterns = compiledPropertiesPatternMap;
    }

    protected int maxDecryptDepth = 0;

    public int getMaxDecryptDepth() {
        return maxDecryptDepth;
    }

    public void setMaxDecryptDepth(int maxDecryptDepth) {
        if (maxDecryptDepth < 0) {
            this.maxDecryptDepth = -1;
        } else {
            this.maxDecryptDepth = maxDecryptDepth;
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public UniqueAlltimeID _getId() {
        return id;
    }

    public void setId(long id) {
        this.id.setID(id);
    }

    public long getId() {
        return id.getID();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPattern() {
        final Pattern lPattern = _getPattern();
        if (lPattern != null) {
            return lPattern.pattern();
        } else {
            return null;
        }
    }

    public boolean updateCookies(final Browser br, final String url, final boolean forceUpdate, final boolean onlyOnPatternMatch) {
        if ((!onlyOnPatternMatch || matches(url)) && (forceUpdate || isUpdateCookies())) {
            final Cookies cookies = br.getCookies(url);
            final List<String[]> updateCookies = new ArrayList<String[]>();
            for (final Cookie cookie : cookies.getCookies()) {
                if (!cookie.isExpired()) {
                    updateCookies.add(new String[] { cookie.getKey(), cookie.getValue() });
                }
            }
            setCookiesList(updateCookies);
            // TODO: add support for length==3, url pattern matching support
            return true;
        }
        return false;
    }

    public void applyCookiesAndHeaders(final Browser br, final String url, final boolean onlyOnPatternMatch) {
        applyCookies(br, url, onlyOnPatternMatch);
        applyHeaders(br, url);
    }

    public boolean applyCookies(final Browser br, final String url, final boolean onlyOnPatternMatch) {
        if (onlyOnPatternMatch && !matches(url)) {
            return false;
        }
        final List<String[]> cookies = getCookiesList();
        if (cookies == null || cookies.size() == 0) {
            return false;
        }
        int cookiesSet = 0;
        for (final String cookie[] : cookies) {
            if (cookie == null) {
                continue;
            }
            switch (cookie.length) {
            case 1:
                br.setCookie(url, cookie[0], null);
                cookiesSet++;
                break;
            case 2:
                br.setCookie(url, cookie[0], cookie[1]);
                cookiesSet++;
                break;
            case 3:
                try {
                    if (cookie[2] != null && url.matches(cookie[2])) {
                        br.setCookie(url, cookie[0], cookie[1]);
                        cookiesSet++;
                    }
                } catch (final Exception e) {
                    final LogInterface logger = br.getLogger();
                    if (logger != null) {
                        logger.log(e);
                    }
                }
                break;
            default:
                break;
            }
        }
        if (cookiesSet > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean applyHeaders(final Browser br, final String url) {
        final List<String[]> headers = getHeaders();
        if (headers == null || headers.size() == 0) {
            return false;
        }
        int headersSet = 0;
        for (final String header[] : headers) {
            if (header == null) {
                continue;
            } else if (header.length != 2) {
                continue;
            } else if (header[0] == null) {
                continue;
            }
            br.getHeaders().put(header[0], header[1]);
            headersSet++;
        }
        if (headersSet > 0) {
            return true;
        } else {
            return false;
        }
    }

    public boolean matches(final String input) {
        final Pattern lPattern = _getPattern();
        if (lPattern == null || input == null) {
            return false;
        } else if (lPattern.matcher(input).matches()) {
            return true;
        } else {
            try {
                final URL url = new URL(input);
                if (url.getUserInfo() != null) {
                    return lPattern.matcher(URLHelper.getURL(url, true, false, true).toString()).matches();
                } else {
                    return false;
                }
            } catch (final Throwable ignore) {
                return false;
            }
        }
    }

    public Pattern _getPattern() {
        return pattern;
    }

    public void setPattern(String pattern) {
        if (pattern == null) {
            this.pattern = null;
        } else {
            this.pattern = Pattern.compile(pattern);
        }
    }

    public RULE getRule() {
        return rule;
    }

    public void setRule(RULE rule) {
        this.rule = rule;
    }

    public LinkCrawlerRule() {
        this.id = new UniqueAlltimeID();
    }

    protected LinkCrawlerRule(long ID) {
        this.id = new UniqueAlltimeID(ID);
    }

    protected final UniqueAlltimeID id;
    protected String                name               = null;
    protected Pattern               pattern            = null;
    protected RULE                  rule               = null;
    protected Pattern               packageNamePattern = null;
    protected Pattern               passwordPattern    = null;
    protected Pattern               formPattern        = null;
    protected Pattern               deepPattern        = null;
    protected String                rewriteReplaceWith = null;

    public Pattern _getPasswordPattern() {
        return passwordPattern;
    }

    public String getPasswordPattern() {
        final Pattern lPattern = _getPasswordPattern();
        if (lPattern != null) {
            return lPattern.pattern();
        } else {
            return null;
        }
    }

    public void setPasswordPattern(String pattern) {
        if (pattern == null) {
            this.passwordPattern = null;
        } else {
            this.passwordPattern = Pattern.compile(pattern);
        }
    }

    public String getRewriteReplaceWith() {
        return rewriteReplaceWith;
    }

    public void setRewriteReplaceWith(String rewriteReplaceWith) {
        this.rewriteReplaceWith = rewriteReplaceWith;
    }

    public Pattern _getDeepPattern() {
        return deepPattern;
    }

    public String getDeepPattern() {
        final Pattern lPattern = _getDeepPattern();
        if (lPattern != null) {
            return lPattern.pattern();
        } else {
            return null;
        }
    }

    public void setDeepPattern(String pattern) {
        if (pattern == null) {
            this.deepPattern = null;
        } else {
            this.deepPattern = Pattern.compile(pattern);
        }
    }

    public Pattern _getPackageNamePattern() {
        return packageNamePattern;
    }

    public Pattern _getFormPattern() {
        return formPattern;
    }

    public String getFormPattern() {
        final Pattern lPattern = _getFormPattern();
        if (lPattern != null) {
            return lPattern.pattern();
        } else {
            return null;
        }
    }

    public void setFormPattern(String pattern) {
        if (pattern == null) {
            this.formPattern = null;
        } else {
            this.formPattern = Pattern.compile(pattern);
        }
    }

    public String getPackageNamePattern() {
        final Pattern lPattern = _getPackageNamePattern();
        if (lPattern != null) {
            return lPattern.pattern();
        } else {
            return null;
        }
    }

    public void setPackageNamePattern(String pattern) {
        if (pattern == null) {
            this.packageNamePattern = null;
        } else {
            this.packageNamePattern = Pattern.compile(pattern);
        }
    }
}
