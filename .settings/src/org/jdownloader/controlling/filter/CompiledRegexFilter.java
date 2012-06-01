package org.jdownloader.controlling.filter;

import java.util.regex.Pattern;

public class CompiledRegexFilter extends RegexFilter {

    private Pattern pattern;

    public CompiledRegexFilter(RegexFilter filter) {
        super(filter.enabled, filter.getMatchType(), filter.getRegex(), filter.isUseRegex());
        pattern = LinkgrabberFilterRuleWrapper.createPattern(filter.getRegex(), filter.isUseRegex());
    }

    public Pattern getPattern() {
        return pattern;
    }

    public boolean matches(String string) {

        switch (getMatchType()) {
        case CONTAINS:
            return pattern.matcher(string).find();
        case EQUALS:
            return pattern.matcher(string).matches();
        case CONTAINS_NOT:
            return !pattern.matcher(string).find();
        case EQUALS_NOT:
            return !pattern.matcher(string).matches();
        }
        return false;

    }
}
