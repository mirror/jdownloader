package org.jdownloader.controlling.filter;

import org.appwork.storage.Storable;
import org.appwork.storage.config.JsonConfig;
import org.jdownloader.gui.translate._GUI;

public class RegexFilter extends Filter implements Storable {
    private RegexFilter() {
        // Storable
    }

    public String toString() {
        if (matchType == MatchType.CONTAINS) {
            return _GUI._.RegexFilter_toString_contains(regex);
        } else {
            return _GUI._.RegexFilter_toString_matches(regex);
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
        CONTAINS, EQUALS
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
