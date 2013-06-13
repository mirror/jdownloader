package jd.gui.swing.jdgui.components.toolbar.actions;

import org.jdownloader.gui.toolbar.action.AbstractToolbarToggleAction;
import org.jdownloader.gui.translate._GUI;
import org.jdownloader.gui.views.SelectionInfo;

public class SilentModeToggleAction extends AbstractToolbarToggleAction {

    public SilentModeToggleAction(SelectionInfo<?, ?> selection) {
        super(org.jdownloader.settings.staticreferences.CFG_GUI.MANUAL_SILENT_MODE_ENABLED);
        setIconKey("silentmode");
    }

    @Override
    protected String createTooltip() {
        return _GUI._.action_silentmode_tooltip();
    }

}
