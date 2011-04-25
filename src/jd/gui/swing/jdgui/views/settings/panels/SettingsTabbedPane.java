package jd.gui.swing.jdgui.views.settings.panels;

import javax.swing.ImageIcon;
import javax.swing.JTabbedPane;

public class SettingsTabbedPane extends JTabbedPane {
    public SettingsTabbedPane() {
        super();
    }

    public void addTab(SettingsTab component, ImageIcon icon, String label) {
        this.addTab(label, icon, component, label);

    }
}
