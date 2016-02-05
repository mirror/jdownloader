package org.jdownloader.controlling.filter;

import java.util.regex.Pattern;

public class CompiledRegexFilter extends RegexFilter {

    private final Pattern pattern;

    public CompiledRegexFilter(RegexFilter filter) {
        super(filter.enabled, filter.getMatchType(), filter.getRegex(), filter.isUseRegex());
        pattern = LinkgrabberFilterRuleWrapper.createPattern(filter.getRegex(), filter.isUseRegex());
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean matches(String string) {
        final boolean ret;
        switch (getMatchType()) {
        case CONTAINS:
            ret = pattern.matcher(string).find();
            break;
        case EQUALS:
            ret = pattern.matcher(string).matches();
            break;
        case CONTAINS_NOT:
            ret = !pattern.matcher(string).find();
            break;
        case EQUALS_NOT:
            ret = !pattern.matcher(string).matches();
            break;
        default:
            ret = false;
            break;
        }
        return ret;

    }
}
