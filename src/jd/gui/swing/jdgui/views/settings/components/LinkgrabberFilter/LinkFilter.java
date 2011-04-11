package jd.gui.swing.jdgui.views.settings.components.LinkgrabberFilter;

public class LinkFilter {

    enum Types {
        URL, PLUGIN, FILENAME

    }

    private boolean enabled;

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
