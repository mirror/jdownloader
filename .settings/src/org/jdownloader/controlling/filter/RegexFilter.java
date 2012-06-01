package org.jdownloader.controlling.filter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.appwork.storage.Storable;
import org.jdownloader.gui.translate._GUI;

public class RegexFilter extends Filter implements Storable {
    public RegexFilter() {
        // Storable
    }

    public String toString() {
        switch (getMatchType()) {
        case CONTAINS:
            return _GUI._.RegexFilter_toString_contains(regex);
        case CONTAINS_NOT:
            return _GUI._.RegexFilter_toString_contains_not(regex);
        case EQUALS:
            return _GUI._.RegexFilter_toString_matches(regex);
        default:
            return _GUI._.RegexFilter_toString_matches_not(regex);
        }

    }

    public MatchType getMatchType() {
        return matchType;
    }

    public void setMatchType(MatchType matchType) {
        this.matchType = matchType;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    private MatchType matchType = MatchType.CONTAINS;
    private String    regex;
    private boolean   useRegex;

    public RegexFilter(boolean enabled, MatchType matchType, String text, boolean regex2) {
        this.enabled = enabled;
        this.matchType = matchType;
        this.regex = text;
        this.useRegex = regex2;
    }

    public boolean isUseRegex() {
        return useRegex;
    }

    public void setUseRegex(boolean useRegex) {
        this.useRegex = useRegex;
    }

    public static enum MatchType {
        CONTAINS,
        EQUALS,
        CONTAINS_NOT,
        EQUALS_NOT
    }

    public int calcPlaceholderCount() {
        int i = 0;
        try {
            Matcher matcher = Pattern.compile("\\(.*?\\)", Pattern.CASE_INSENSITIVE).matcher(LinkgrabberFilterRuleWrapper.createPattern(regex, useRegex).pattern());
            while (matcher.find()) {
                // System.out.println(matcher.group());
                i++;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return i;

    }

    public boolean matches(String downloadURL) {
        return false;
    }
}
