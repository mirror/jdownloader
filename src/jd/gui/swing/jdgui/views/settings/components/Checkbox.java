package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JCheckBox;

public class Checkbox extends JCheckBox implements SettingsComponent {

    private static final long serialVersionUID = -4399955553684242527L;

    public Checkbox(boolean selected) {
        super();
        this.setSelected(selected);
    }

    public Checkbox() {
        super();
    }

    public String getConstraints() {
        return null;
    }

    public boolean isMultiline() {
        return false;
    }

}
