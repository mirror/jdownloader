package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

import org.appwork.storage.Storable;

public class LinkFilter implements Storable {

    enum Types {
        URL, PLUGIN, FILENAME

    }

    private LinkFilter() {
        // required by Storable
    }

    private boolean blacklist = true;

    public boolean isBlacklist() {
        return blacklist;
    }

    public void setBlacklist(boolean blacklist) {
        this.blacklist = blacklist;
    }

    private boolean enabled;
    private boolean fullRegex;

    public boolean isFullRegex() {
        return fullRegex;
    }

    public void setFullRegex(boolean fullRegex) {
        this.fullRegex = fullRegex;
    }

    public boolean isCaseSensitive() {
        return caseSensitive;
    }

    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }

    private boolean caseSensitive;

    public Types getType() {
        return type;
    }

    public void setType(Types type) {
        this.type = type;
    }

    public String getRegex() {
        return regex;
    }

    public void setRegex(String regex) {
        this.regex = regex;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    private Types  type;
    private String regex;

    public LinkFilter(boolean enabled, Types type, String regex) {
        this.enabled = enabled;
        this.type = type;
        this.regex = regex;
    }

    public boolean isEnabled() {
        return enabled;
    }

}
