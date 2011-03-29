package org.jdownloader.extensions.settings;

import javax.swing.JComboBox;

public class ComboBox extends JComboBox implements SettingsComponent {
    public ComboBox(String... options) {
        super(options);
        // this.setSelectedIndex(selection);
    }

    public String getConstraints() {
        return null;
    }
}
