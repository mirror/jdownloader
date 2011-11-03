package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;

public class RegexFilter extends Filter implements Storable {
    private RegexFilter() {
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

    private MatchType matchType;
    private String    regex;

    public RegexFilter(boolean enabled, MatchType matchType, String text) {
        this.enabled = enabled;
        this.matchType = matchType;
        this.regex = text;
    }

    public static enum MatchType {
        CONTAINS,
        EQUALS,
        CONTAINS_NOT,
        EQUALS_NOT
    }

    public int calcPlaceholderCount() {
        if (JsonConfig.create(LinkFilterSettings.class).isRuleconditionsRegexEnabled()) {
            return (" " + regex + " ").split("\\(.*?\\)").length - 1;
        } else {
            return (" " + regex + " ").split("\\*").length - 1;
        }

    }

    public boolean matches(String downloadURL) {
        return false;
    }
}
