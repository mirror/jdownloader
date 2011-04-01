package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JComboBox;

public class ComboBox extends JComboBox implements SettingsComponent {
    public ComboBox(String... options) {
        super(options);
        // this.setSelectedIndex(selection);
    }

    public String getConstraints() {
        return null;
    }

    public boolean isMultiline() {
        return false;
    }
}
