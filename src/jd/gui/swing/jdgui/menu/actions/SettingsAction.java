package jd.gui.swing.jdgui.menu.actions;

import java.awt.event.ActionEvent;

import jd.gui.swing.SwingGui;
import jd.gui.swing.jdgui.actions.ToolBarAction;
import jd.gui.swing.jdgui.views.settings.ConfigurationView;

public class SettingsAction extends ToolBarAction {

    private static final long serialVersionUID = 2547991585530678706L;

    public SettingsAction() {
        super("action.settings", "gui.images.taskpanes.configuration");
    }

    @Override
    public void onAction(ActionEvent e) {
        SwingGui.getInstance().setContent(ConfigurationView.getInstance());
    }

    @Override
    public void init() {
    }

    @Override
    public void initDefaults() {
    }

}
