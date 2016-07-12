package jd.gui.swing.jdgui.views.settings.components;

import javax.swing.JComponent;

import org.appwork.swing.MigPanel;

public class SettingsContainer extends MigPanel implements SettingsComponent {
    public SettingsContainer(JComponent com) {
        super("ins 0", "[]", "[]");
        add(com);
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
