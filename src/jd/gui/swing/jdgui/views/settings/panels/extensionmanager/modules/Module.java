package jd.gui.swing.jdgui.views.settings.panels.extensionmanager.modules;

import org.appwork.swing.MigPanel;

public abstract class Module extends MigPanel {

    public Module(String constraints, String columns, String rows) {
        super(constraints, columns, rows);
        setOpaque(false);
        setBackground(null);
    }

}
