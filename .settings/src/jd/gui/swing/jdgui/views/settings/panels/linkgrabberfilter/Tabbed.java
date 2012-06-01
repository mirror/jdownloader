package jd.gui.swing.jdgui.views.settings.panels.linkgrabberfilter;

import javax.swing.JTabbedPane;

import jd.gui.swing.jdgui.views.settings.components.SettingsComponent;
import jd.gui.swing.jdgui.views.settings.components.StateUpdateListener;

public class Tabbed extends JTabbedPane implements SettingsComponent {
    private static final long serialVersionUID = 6070464296168772795L;

    public Tabbed() {
        super();

    }

    // public FilterTable getTable() {
    // return table;
    // }

    public String getConstraints() {

        return "height 60:n:n,pushy,growy";
    }

    public boolean isMultiline() {
        return true;
    }

    public void addStateUpdateListener(StateUpdateListener listener) {
        throw new IllegalStateException("Not implemented");
    }
}
