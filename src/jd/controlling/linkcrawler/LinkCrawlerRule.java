package jd.controlling.linkcrawler;

import java.util.regex.Pattern;

import org.jdownloader.controlling.UniqueAlltimeID;

public class LinkCrawlerRule {

    public static enum RULE {
        DIRECTHTTP,
        DEEPDECRYPT
    }

    protected boolean enabled = true;

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
        return lPattern != null && lPattern.matcher(input).matches();
    }

    public Pattern _getPattern() {
        return pattern;
    }

    public void _setPattern(Pattern pattern) {
        this.pattern = pattern;
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
    protected String                name    = null;
    protected Pattern               pattern = null;
    protected RULE                  rule    = null;
}
