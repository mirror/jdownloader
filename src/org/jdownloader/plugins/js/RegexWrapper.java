package org.jdownloader.plugins.js;

import org.appwork.utils.Regex;

public class RegexWrapper {

    private Regex regex;

    public RegexWrapper(String src, String pattern) {
        regex = new Regex(src, pattern);
    }

    public String getMatch(int index) {
        return regex.getMatch(index);
    }
}
