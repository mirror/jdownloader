package jd.controlling.linkcrawler;

import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;

import org.appwork.utils.net.URLHelper;
import org.jdownloader.controlling.UniqueAlltimeID;

public class LinkCrawlerRule {
    public static enum RULE {
        REWRITE,
        SUBMITFORM,
        DIRECTHTTP,
        DEEPDECRYPT,
        FOLLOWREDIRECT
    }

    protected boolean        enabled = true;
    protected List<String[]> cookies = null;

    public List<String[]> getCookies() {
        return cookies;
    }

    public void setCookies(List<String[]> cookies) {
        this.cookies = cookies;
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

    public boolean matches(final String input) {
        final Pattern lPattern = _getPattern();
        if (lPattern == null) {
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
    protected Pattern               formPattern        = null;
    protected Pattern               deepPattern        = null;
    protected String                rewriteReplaceWith = null;

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
