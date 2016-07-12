package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class Label extends JLabel implements SettingsComponent {
    public Label(String txt, Icon icon) {
        super(txt, icon, SwingConstants.LEFT);
    }

    public Label(String txt) {
        super(txt);
    }

    @Override
    public String getConstraints() {
        return null;
    }

    @Override
    public boolean isMultiline() {
        return false;
    }
}
